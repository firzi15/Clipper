package com.clipper.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clipper.app.auth.SessionManager
import com.clipper.app.downloader.YouTubeStreamExtractor
import com.clipper.app.nlm.NlmClient
import com.clipper.app.nlm.models.CropMode
import com.clipper.app.nlm.models.ViralSegment
import com.clipper.app.video.VideoCompositor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface AppState {
    data object Idle : AppState
    data class Processing(val stepTitle: String, val progress: Float) : AppState
    data class SegmentsReady(
        val segments: List<ViralSegment>,
        val selectedIndex: Int = 0,
        val localVideoFile: File? = null,
        val currentCropMode: CropMode = CropMode.AUTO
    ) : AppState
    data class Exporting(val progress: Float) : AppState
    data class ExportCompleted(val outputFile: File) : AppState
    data class Error(val message: String) : AppState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val sessionManager = SessionManager(application)
    private val nlmClient = NlmClient(sessionManager)
    private val extractor = YouTubeStreamExtractor(application)
    private val compositor = VideoCompositor(application)

    private val _appState = MutableStateFlow<AppState>(AppState.Idle)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    fun openLogin() {
        _showLoginDialog.value = true
    }

    fun dismissLogin() {
        _showLoginDialog.value = false
    }

    fun onLoginSuccess(cookies: String, csrfToken: String?) {
        sessionManager.saveCookies(cookies)
        csrfToken?.let { sessionManager.saveCsrfToken(it) }
        _showLoginDialog.value = false
    }

    fun processYouTubeUrl(url: String) {
        if (url.isBlank()) {
            _appState.value = AppState.Error("Masukkan URL YouTube terlebih dahulu.")
            return
        }

        viewModelScope.launch {
            _appState.value = AppState.Processing("Memulai analisis video...", 0.05f)

            // Step 1: Query NotebookLM for viral hooks & timestamps
            val result = nlmClient.analyzeYoutubeVideo(url) { step, progress ->
                _appState.value = AppState.Processing(step, progress)
            }

            result.onSuccess { segments ->
                if (segments.isEmpty()) {
                    _appState.value = AppState.Error("Tidak ada segmen viral yang terdeteksi.")
                    return@launch
                }

                _appState.value = AppState.Processing("Menyiapkan preview video...", 0.98f)
                val primarySegment = segments.first()

                // Step 2: Download preview segment buffer
                val streamUrl = extractor.resolveStreamUrl(url)
                val file = extractor.prepareSegmentFile(
                    streamUrl = streamUrl,
                    startSec = primarySegment.startSeconds,
                    endSec = primarySegment.endSeconds
                ) { /* download progress */ }

                _appState.value = AppState.SegmentsReady(
                    segments = segments,
                    selectedIndex = 0,
                    localVideoFile = file,
                    currentCropMode = primarySegment.recommendedCrop
                )
            }.onFailure { err ->
                _appState.value = AppState.Error(err.message ?: "Terjadi kesalahan saat memproses.")
            }
        }
    }

    fun selectSegment(index: Int) {
        val current = _appState.value as? AppState.SegmentsReady ?: return
        if (index in current.segments.indices) {
            val segment = current.segments[index]
            _appState.value = current.copy(
                selectedIndex = index,
                currentCropMode = segment.recommendedCrop
            )
        }
    }

    fun setCropMode(mode: CropMode) {
        val current = _appState.value as? AppState.SegmentsReady ?: return
        _appState.value = current.copy(currentCropMode = mode)
    }

    fun startExport() {
        val current = _appState.value as? AppState.SegmentsReady ?: return
        val segment = current.segments[current.selectedIndex]
        val file = current.localVideoFile ?: return

        viewModelScope.launch {
            _appState.value = AppState.Exporting(0f)
            val exportResult = compositor.exportVideo(
                sourceFile = file,
                segment = segment,
                cropMode = current.currentCropMode
            ) { progress ->
                _appState.value = AppState.Exporting(progress)
            }

            exportResult.onSuccess { outputFile ->
                _appState.value = AppState.ExportCompleted(outputFile)
            }.onFailure { error ->
                _appState.value = AppState.Error("Gagal export video: ${error.message}")
            }
        }
    }

    fun resetToHome() {
        _appState.value = AppState.Idle
    }
}
