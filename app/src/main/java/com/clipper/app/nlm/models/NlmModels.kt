package com.clipper.app.nlm.models

import kotlinx.serialization.Serializable

@Serializable
data class ViralSegment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startSeconds: Long,
    val endSeconds: Long,
    val startTimeFormatted: String, // e.g. "01:25"
    val endTimeFormatted: String,   // e.g. "02:15"
    val hookHeadline: String,       // Short attention-grabbing title
    val viralScore: Int,            // 1-100 score
    val rationale: String,          // Why this clip is viral
    val transcript: String,         // Full dialogue/transcript of this segment
    val recommendedCrop: CropMode = CropMode.AUTO
) {
    val durationSeconds: Long
        get() = endSeconds - startSeconds
}

enum class CropMode {
    AUTO,
    SINGLE_SPEAKER,
    PODCAST_SPLIT
}

@Serializable
data class NotebookSession(
    val notebookId: String,
    val title: String,
    val sourceId: String? = null
)
