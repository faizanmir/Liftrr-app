package org.liftrr.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ml.PoseLandmarks

/**
 * Renders skeletal overlay on workout frames
 */
object SkeletalOverlayRenderer {

    private const val LANDMARK_RADIUS = 8f
    private const val CONNECTION_STROKE_WIDTH = 6f

    // Pose connections (body parts to connect with lines)
    private val POSE_CONNECTIONS = listOf(
        // Face
        PoseLandmarks.LEFT_EYE to PoseLandmarks.RIGHT_EYE,
        PoseLandmarks.LEFT_EYE to PoseLandmarks.NOSE,
        PoseLandmarks.RIGHT_EYE to PoseLandmarks.NOSE,
        PoseLandmarks.LEFT_EYE to PoseLandmarks.LEFT_EAR,
        PoseLandmarks.RIGHT_EYE to PoseLandmarks.RIGHT_EAR,

        // Torso
        PoseLandmarks.LEFT_SHOULDER to PoseLandmarks.RIGHT_SHOULDER,
        PoseLandmarks.LEFT_SHOULDER to PoseLandmarks.LEFT_HIP,
        PoseLandmarks.RIGHT_SHOULDER to PoseLandmarks.RIGHT_HIP,
        PoseLandmarks.LEFT_HIP to PoseLandmarks.RIGHT_HIP,

        // Left arm
        PoseLandmarks.LEFT_SHOULDER to PoseLandmarks.LEFT_ELBOW,
        PoseLandmarks.LEFT_ELBOW to PoseLandmarks.LEFT_WRIST,
        PoseLandmarks.LEFT_WRIST to PoseLandmarks.LEFT_THUMB,
        PoseLandmarks.LEFT_WRIST to PoseLandmarks.LEFT_PINKY,

        // Right arm
        PoseLandmarks.RIGHT_SHOULDER to PoseLandmarks.RIGHT_ELBOW,
        PoseLandmarks.RIGHT_ELBOW to PoseLandmarks.RIGHT_WRIST,
        PoseLandmarks.RIGHT_WRIST to PoseLandmarks.RIGHT_THUMB,
        PoseLandmarks.RIGHT_WRIST to PoseLandmarks.RIGHT_PINKY,

        // Left leg
        PoseLandmarks.LEFT_HIP to PoseLandmarks.LEFT_KNEE,
        PoseLandmarks.LEFT_KNEE to PoseLandmarks.LEFT_ANKLE,
        PoseLandmarks.LEFT_ANKLE to PoseLandmarks.LEFT_HEEL,
        PoseLandmarks.LEFT_ANKLE to PoseLandmarks.LEFT_FOOT_INDEX,

        // Right leg
        PoseLandmarks.RIGHT_HIP to PoseLandmarks.RIGHT_KNEE,
        PoseLandmarks.RIGHT_KNEE to PoseLandmarks.RIGHT_ANKLE,
        PoseLandmarks.RIGHT_ANKLE to PoseLandmarks.RIGHT_HEEL,
        PoseLandmarks.RIGHT_ANKLE to PoseLandmarks.RIGHT_FOOT_INDEX
    )

    /**
     * Draw skeletal overlay on a bitmap
     */
    fun drawSkeletonOnBitmap(
        originalBitmap: Bitmap,
        pose: PoseDetectionResult.Success,
        goodForm: Boolean = true
    ): Bitmap {
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val lineColor = if (goodForm) Color.GREEN else Color.RED
        val pointColor = if (goodForm) Color.GREEN else Color.YELLOW

        val linePaint = Paint().apply {
            color = lineColor
            strokeWidth = CONNECTION_STROKE_WIDTH
            style = Paint.Style.STROKE
            isAntiAlias = true
            alpha = 200
        }

        val pointPaint = Paint().apply {
            color = pointColor
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = 220
        }

        val width = mutableBitmap.width.toFloat()
        val height = mutableBitmap.height.toFloat()

        // Draw connections (lines between joints)
        POSE_CONNECTIONS.forEach { (startIdx, endIdx) ->
            val startLandmark = pose.getLandmark(startIdx)
            val endLandmark = pose.getLandmark(endIdx)

            if (startLandmark != null && endLandmark != null) {
                val startX = startLandmark.x() * width
                val startY = startLandmark.y() * height
                val endX = endLandmark.x() * width
                val endY = endLandmark.y() * height

                // Only draw if both points have good visibility
                val startVis = startLandmark.visibility()?.get() ?: 0f
                val endVis = endLandmark.visibility()?.get() ?: 0f

                if (startVis > 0.5f && endVis > 0.5f) {
                    canvas.drawLine(startX, startY, endX, endY, linePaint)
                }
            }
        }

        // Draw landmarks (points at joints)
        pose.landmarks.forEachIndexed { index, landmark ->
            val visibility = landmark.visibility()?.get() ?: 0f
            if (visibility > 0.5f) {
                val x = landmark.x() * width
                val y = landmark.y() * height
                canvas.drawCircle(x, y, LANDMARK_RADIUS, pointPaint)
            }
        }

        return mutableBitmap
    }

    /**
     * Draw skeletal overlay with form issue annotations
     */
    fun drawSkeletonWithAnnotations(
        originalBitmap: Bitmap,
        pose: PoseDetectionResult.Success,
        formIssues: List<String>,
        goodForm: Boolean = true
    ): Bitmap {
        val bitmap = drawSkeletonOnBitmap(originalBitmap, pose, goodForm)
        val canvas = Canvas(bitmap)

        // Add text annotations for form issues
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val backgroundPaint = Paint().apply {
            color = if (goodForm) Color.argb(180, 0, 150, 0)
                    else Color.argb(180, 200, 0, 0)
            style = Paint.Style.FILL
        }

        // Draw form status at top
        val statusText = if (goodForm) "Good Form" else "Form Issues"
        val statusWidth = textPaint.measureText(statusText)
        canvas.drawRoundRect(
            bitmap.width - statusWidth - 40f,
            20f,
            bitmap.width - 20f,
            80f,
            10f, 10f,
            backgroundPaint
        )
        canvas.drawText(statusText, bitmap.width - statusWidth - 30f, 60f, textPaint)

        // Draw form issues if any
        if (formIssues.isNotEmpty() && !goodForm) {
            var yOffset = 120f
            formIssues.take(2).forEach { issue ->  // Show max 2 issues
                val issueWidth = textPaint.measureText(issue)
                canvas.drawRoundRect(
                    bitmap.width - issueWidth - 40f,
                    yOffset,
                    bitmap.width - 20f,
                    yOffset + 60f,
                    10f, 10f,
                    backgroundPaint
                )
                canvas.drawText(issue, bitmap.width - issueWidth - 30f, yOffset + 40f, textPaint)
                yOffset += 80f
            }
        }

        return bitmap
    }
}
