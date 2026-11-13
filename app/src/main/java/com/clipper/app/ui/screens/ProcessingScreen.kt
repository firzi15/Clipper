package com.clipper.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clipper.app.ui.theme.NeonGreen
import com.clipper.app.ui.theme.NeonYellow

@Composable
fun ProcessingScreen(
    stepTitle: String,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progressAnim"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Center AI Spinner
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(110.dp),
                color = NeonYellow,
                trackColor = Color(0xFF2B2B36),
                strokeWidth = 6.dp
            )
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = NeonYellow,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stepTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Steps Checklist
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            StepItem(
                icon = Icons.Default.VideoLibrary,
                title = "Ingest Transkrip YouTube (NotebookLM)",
                isActive = animatedProgress >= 0.2f,
                isCompleted = animatedProgress >= 0.45f
            )
            StepItem(
                icon = Icons.Default.AutoAwesome,
                title = "Analisis Hook & Skor Viralitas",
                isActive = animatedProgress >= 0.45f,
                isCompleted = animatedProgress >= 0.75f
            )
            StepItem(
                icon = Icons.Default.Face,
                title = "MediaPipe Smart Face Tracking & Split",
                isActive = animatedProgress >= 0.75f,
                isCompleted = animatedProgress >= 0.90f
            )
            StepItem(
                icon = Icons.Default.Subtitles,
                title = "Sinkronisasi Subtitle Dinamis 9:16",
                isActive = animatedProgress >= 0.90f,
                isCompleted = animatedProgress >= 1.0f
            )
        }
    }
}

@Composable
private fun StepItem(
    icon: ImageVector,
    title: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                isCompleted -> NeonGreen
                isActive -> NeonYellow
                else -> Color.DarkGray
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.padding(start = 12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            color = when {
                isCompleted -> Color.White
                isActive -> NeonYellow
                else -> Color.Gray
            },
            fontWeight = if (isActive || isCompleted) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
