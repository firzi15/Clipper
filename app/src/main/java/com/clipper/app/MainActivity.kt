package com.clipper.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.clipper.app.auth.GoogleLoginDialog
import com.clipper.app.ui.AppState
import com.clipper.app.ui.MainViewModel
import com.clipper.app.ui.screens.EditorPreviewScreen
import com.clipper.app.ui.screens.HomeScreen
import com.clipper.app.ui.screens.ProcessingScreen
import com.clipper.app.ui.theme.ClipperTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle YouTube Share Intent (e.g. user taps "Share" on YouTube app)
        handleIncomingYouTubeIntent(intent)

        setContent {
            ClipperTheme {
                val appState by viewModel.appState.collectAsState()
                val showLoginDialog by viewModel.showLoginDialog.collectAsState()
                val isLoggedIn by viewModel.sessionManager.isLoggedIn.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (val state = appState) {
                        is AppState.Idle -> {
                            HomeScreen(
                                isLoggedIn = isLoggedIn,
                                onLoginClick = { viewModel.openLogin() },
                                onSubmitUrl = { url -> viewModel.processYouTubeUrl(url) }
                            )
                        }

                        is AppState.Processing -> {
                            ProcessingScreen(
                                stepTitle = state.stepTitle,
                                progress = state.progress
                            )
                        }

                        is AppState.SegmentsReady -> {
                            EditorPreviewScreen(
                                segments = state.segments,
                                selectedIndex = state.selectedIndex,
                                currentCropMode = state.currentCropMode,
                                localVideoFile = state.localVideoFile,
                                onSelectSegment = { viewModel.selectSegment(it) },
                                onChangeCropMode = { viewModel.setCropMode(it) },
                                onExportClick = { viewModel.startExport() },
                                onBackClick = { viewModel.resetToHome() }
                            )
                        }

                        is AppState.Exporting -> {
                            ProcessingScreen(
                                stepTitle = "Me-render video vertikal 9:16 (1080x1920)...",
                                progress = state.progress
                            )
                        }

                        is AppState.ExportCompleted -> {
                            LaunchedEffect(state) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Video berhasil diekspor ke: ${state.outputFile.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            EditorPreviewScreen(
                                segments = (viewModel.appState.value as? AppState.SegmentsReady)?.segments ?: emptyList(),
                                selectedIndex = 0,
                                currentCropMode = com.clipper.app.nlm.models.CropMode.AUTO,
                                localVideoFile = state.outputFile,
                                onSelectSegment = { viewModel.selectSegment(it) },
                                onChangeCropMode = { viewModel.setCropMode(it) },
                                onExportClick = { viewModel.startExport() },
                                onBackClick = { viewModel.resetToHome() }
                            )
                        }

                        is AppState.Error -> {
                            LaunchedEffect(state.message) {
                                Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetToHome()
                            }
                        }
                    }

                    // Google Login In-App WebView Dialog
                    if (showLoginDialog) {
                        GoogleLoginDialog(
                            onDismiss = { viewModel.dismissLogin() },
                            onLoginSuccess = { cookies, csrfToken ->
                                viewModel.onLoginSuccess(cookies, csrfToken)
                                Toast.makeText(this@MainActivity, "Berhasil terhubung ke Google NotebookLM!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingYouTubeIntent(it) }
    }

    private fun handleIncomingYouTubeIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val ytUrlMatch = Regex("(https?:\\/\\/(?:www\\.)?(?:youtube\\.com|youtu\\.be)\\S+)").find(sharedText)
            val detectedUrl = ytUrlMatch?.value
            if (!detectedUrl.isNullOrBlank()) {
                viewModel.processYouTubeUrl(detectedUrl)
            }
        }
    }
}
