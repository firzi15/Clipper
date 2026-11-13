package com.clipper.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CropOriginal
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clipper.app.nlm.models.CropMode
import com.clipper.app.nlm.models.ViralSegment
import com.clipper.app.subtitle.SubtitleEngine
import com.clipper.app.subtitle.SubtitlePhrase
import com.clipper.app.ui.theme.NeonGreen
import com.clipper.app.ui.theme.NeonYellow
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun EditorPreviewScreen(
    segments: List<ViralSegment>,
    selectedIndex: Int,
    currentCropMode: CropMode,
    localVideoFile: File?,
    onSelectSegment: (Int) -> Unit,
    onChangeCropMode: (CropMode) -> Unit,
    onExportClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val activeSegment = segments.getOrNull(selectedIndex) ?: return

    // Word-level subtitles generation for this segment
    val subtitlePhrases = remember(activeSegment) {
        SubtitleEngine.generatePhrases(
            transcript = activeSegment.transcript.ifBlank {
                "Inilah rahasia kenapa konten lo bisa viral di TikTok dan Shorts!"
            },
            segmentDurationSeconds = activeSegment.durationSeconds.coerceAtLeast(10L)
        )
    }

    // Playback time simulation for live subtitle karaoke animation
    var simulatedPlayheadMs by remember { mutableStateOf(0L) }
    LaunchedEffect(activeSegment) {
        simulatedPlayheadMs = 0L
        while (true) {
            delay(100)
            simulatedPlayheadMs = (simulatedPlayheadMs + 100) % (activeSegment.durationSeconds.coerceAtLeast(10L) * 1000L)
        }
    }

    val (activePhrase, activeWordIdx) = remember(simulatedPlayheadMs, subtitlePhrases) {
        SubtitleEngine.getActiveSubtitleAt(subtitlePhrases, simulatedPlayheadMs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Preview & Smart Crop",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ekspor", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Segments Tabs (Klip 1, Klip 2, etc.)
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = NeonYellow,
            edgePadding = 16.dp
        ) {
            segments.forEachIndexed { index, seg ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onSelectSegment(index) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Klip ${index + 1}")
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${seg.viralScore}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center 9:16 Aspect Ratio Video Preview with Subtitle Burn Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Video Content: Single Speaker vs Podcast Split Screen
                    if (currentCropMode == CropMode.PODCAST_SPLIT) {
                        // Split screen top/bottom view
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top Half (Speaker 1)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E232A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤 Pembicara 1 (Host)", color = Color.LightGray, fontSize = 12.sp)
                            }
                            // Divider line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(NeonYellow)
                            )
                            // Bottom Half (Speaker 2)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFF262C36)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤 Pembicara 2 (Bintang Tamu)", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Single speaker 9:16 centered crop view
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1A1A24)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤 Pembicara (Auto Face Centered)", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }

                    // Watermark / Time chip
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${activeSegment.startTimeFormatted} - ${activeSegment.endTimeFormatted}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dynamic Karaoke Animated Subtitle Overlay (Hormozi Style)
                    if (activePhrase != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                activePhrase.words.forEachIndexed { idx, wordToken ->
                                    val isHighlighted = idx == activeWordIdx
                                    Text(
                                        text = "${wordToken.word} ",
                                        fontSize = if (isHighlighted) 20.sp else 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isHighlighted) NeonYellow else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Controls: Crop Mode Switcher & Segment Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp)
        ) {
            // Hook headline
            Text(
                text = activeSegment.hookHeadline,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activeSegment.rationale,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Crop Mode Selectors
            Text(
                text = "Mode Smart Crop 9:16:",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = currentCropMode == CropMode.SINGLE_SPEAKER || currentCropMode == CropMode.AUTO,
                    onClick = { onChangeCropMode(CropMode.SINGLE_SPEAKER) },
                    label = { Text("1 Orang (Solo)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonYellow,
                        selectedLabelColor = Color.Black
                    )
                )

                FilterChip(
                    selected = currentCropMode == CropMode.PODCAST_SPLIT,
                    onClick = { onChangeCropMode(CropMode.PODCAST_SPLIT) },
                    label = { Text("Podcast 2 Orang (Split Atas-Bawah)") },
                    leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonYellow,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
    }
}
