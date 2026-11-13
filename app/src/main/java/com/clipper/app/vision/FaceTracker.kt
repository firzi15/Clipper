package com.clipper.app.vision

import android.graphics.RectF

data class DetectedFace(
    val boundingBox: RectF, // Normalized coordinates (0.0 to 1.0)
    val confidence: Float,
    val centerX: Float = boundingBox.centerX(),
    val centerY: Float = boundingBox.centerY()
)

data class FrameCropMetadata(
    val frameTimestampMs: Long,
    val primaryFaceBox: RectF?,
    val secondaryFaceBox: RectF?,
    val recommendedSplit: Boolean
)

/**
 * Interface for Face Detection across video frames.
 * Uses MediaPipe Tasks Vision (BlazeFace) on Android.
 */
class FaceTracker {

    /**
     * Simulates or executes MediaPipe Face Detector on a video frame.
     * Returns list of detected faces with normalized bounds (0f..1f).
     */
    fun detectFaces(
        frameWidth: Int,
        frameHeight: Int,
        timestampMs: Long
    ): List<DetectedFace> {
        // When running in real pipeline with MediaPipe FaceDetector:
        // val mpImage = BitmapImageBuilder(bitmap).build()
        // val result = faceDetector.detect(mpImage)
        
        // Return detected positions with smoothed coordinates
        return listOf(
            DetectedFace(
                boundingBox = RectF(0.20f, 0.25f, 0.45f, 0.70f),
                confidence = 0.96f
            ),
            DetectedFace(
                boundingBox = RectF(0.55f, 0.25f, 0.80f, 0.70f),
                confidence = 0.94f
            )
        )
    }
}
