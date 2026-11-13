package com.clipper.app.vision

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * Calculates optimal crop rectangles for 9:16 portrait videos.
 * Handles both Single Speaker tracking and Dual-Speaker Podcast Split-Screen.
 */
object SmartCropStrategy {

    // Target vertical aspect ratio: 9/16 = 0.5625
    const val TARGET_ASPECT_RATIO = 9f / 16f

    // Target split half aspect ratio: 9/8 = 1.125
    const val SPLIT_HALF_ASPECT_RATIO = 9f / 8f

    /**
     * Calculates smooth crop box for a single speaker.
     * Uses Exponential Moving Average (EMA) to prevent jerky camera motion.
     */
    fun calculateSingleSpeakerCrop(
        currentCenterX: Float,
        previousSmoothedX: Float,
        alphaSmoothing: Float = 0.15f
    ): Float {
        // Smooth transition
        val smoothedX = previousSmoothedX + alphaSmoothing * (currentCenterX - previousSmoothedX)
        return smoothedX.coerceIn(0.18f, 0.82f)
    }

    /**
     * Calculates crop rectangle for 16:9 to 9:16.
     * Returns normalized coordinates RectF(left, top, right, bottom).
     */
    fun getSingleSpeakerRect(
        videoAspect: Float = 16f / 9f,
        centerX: Float
    ): RectF {
        // Normalized crop width in 16:9 frame
        val normalizedCropWidth = (1f / videoAspect) * TARGET_ASPECT_RATIO // (9/16) / (16/9) = 81/256 ≈ 0.3164
        val halfWidth = normalizedCropWidth / 2f

        val left = max(0f, min(1f - normalizedCropWidth, centerX - halfWidth))
        val right = left + normalizedCropWidth

        return RectF(left, 0f, right, 1f)
    }

    /**
     * Calculates two crop rectangles for Podcast 2-Speaker Split Screen.
     * Top half focuses on Speaker 1 (usually left person).
     * Bottom half focuses on Speaker 2 (usually right person).
     */
    fun getPodcastSplitRects(
        face1CenterX: Float = 0.25f,
        face2CenterX: Float = 0.75f,
        videoAspect: Float = 16f / 9f
    ): Pair<RectF, RectF> {
        // Normalized crop width for 9:8 ratio
        val normalizedCropWidth = (1f / videoAspect) * SPLIT_HALF_ASPECT_RATIO // (9/8) / (16/9) = 81/128 ≈ 0.6328
        val halfWidth = normalizedCropWidth / 2f

        // Speaker 1 (Top Screen)
        val left1 = max(0f, min(1f - normalizedCropWidth, face1CenterX - halfWidth))
        val right1 = left1 + normalizedCropWidth
        val topSpeakerRect = RectF(left1, 0f, right1, 1f)

        // Speaker 2 (Bottom Screen)
        val left2 = max(0f, min(1f - normalizedCropWidth, face2CenterX - halfWidth))
        val right2 = left2 + normalizedCropWidth
        val bottomSpeakerRect = RectF(left2, 0f, right2, 1f)

        return Pair(topSpeakerRect, bottomSpeakerRect)
    }
}
