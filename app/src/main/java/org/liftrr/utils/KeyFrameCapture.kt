package org.liftrr.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.liftrr.domain.workout.CapturedFrame
import org.liftrr.domain.workout.KeyFrame
import org.liftrr.domain.workout.KeyFrameType
import org.liftrr.ml.PoseDetectionResult
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Handles capturing and saving key frames during workout
 */
class KeyFrameCapture(private val context: Context) {

    private val capturedFrames = mutableListOf<CapturedFrame>()
    private val TAG = "KeyFrameCapture"

    /**
     * Capture a frame during workout
     */
    fun captureFrame(
        bitmap: Bitmap,
        timestamp: Long,
        repNumber: Int,
        poseData: PoseDetectionResult.Success,
        formScore: Float,
        formIssues: List<String>
    ) {
        capturedFrames.add(
            CapturedFrame(
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false),
                timestamp = timestamp,
                repNumber = repNumber,
                poseData = poseData,
                formScore = formScore,
                formIssues = formIssues
            )
        )
    }

    /**
     * Process and save key frames at end of workout
     * Returns list of KeyFrame metadata with minimum 10 frames for quality analysis
     */
    fun processAndSaveKeyFrames(sessionId: String): List<KeyFrame> {
        if (capturedFrames.isEmpty()) {
            Log.w(TAG, "No frames captured")
            return emptyList()
        }

        val keyFrames = mutableListOf<KeyFrame>()
        val savedFrames = mutableSetOf<CapturedFrame>()  // Track already saved frames

        try {
            // Create directory for this session's frames
            val framesDir = File(context.filesDir, "workout_frames/$sessionId")
            if (!framesDir.exists()) {
                framesDir.mkdirs()
            }

            // 1. Best rep (highest form score)
            val bestFrame = capturedFrames.maxByOrNull { it.formScore }
            bestFrame?.let { frame ->
                val savedPath = saveFrameWithOverlay(
                    frame,
                    framesDir,
                    "01_best_rep",
                    goodForm = true
                )
                savedPath?.let {
                    keyFrames.add(
                        KeyFrame(
                            timestamp = frame.timestamp,
                            repNumber = frame.repNumber,
                            frameType = KeyFrameType.BEST_REP,
                            imagePath = it,
                            description = "Best Rep #${frame.repNumber} - Form Score: ${(frame.formScore * 100).toInt()}%",
                            formScore = frame.formScore
                        )
                    )
                    savedFrames.add(frame)
                }
            }

            // 2. Worst rep (lowest form score)
            val worstFrame = capturedFrames.minByOrNull { it.formScore }
            worstFrame?.let { frame ->
                if (frame !in savedFrames) {
                    val savedPath = saveFrameWithOverlay(
                        frame,
                        framesDir,
                        "02_worst_rep",
                        goodForm = false
                    )
                    savedPath?.let {
                        keyFrames.add(
                            KeyFrame(
                                timestamp = frame.timestamp,
                                repNumber = frame.repNumber,
                                frameType = KeyFrameType.WORST_REP,
                                imagePath = it,
                                description = "Worst Rep #${frame.repNumber} - Form Score: ${(frame.formScore * 100).toInt()}%\nIssues: ${frame.formIssues.joinToString(", ")}",
                                formScore = frame.formScore
                            )
                        )
                        savedFrames.add(frame)
                    }
                }
            }

            // 3. Top 4 form issue examples (diverse issues)
            val issueFrames = capturedFrames
                .filter { it.formIssues.isNotEmpty() && it !in savedFrames }
                .sortedBy { it.formScore }  // Worst form issues first
                .take(4)

            issueFrames.forEachIndexed { index, frame ->
                val savedPath = saveFrameWithOverlay(
                    frame,
                    framesDir,
                    "03_form_issue_${index + 1}",
                    goodForm = false
                )
                savedPath?.let {
                    keyFrames.add(
                        KeyFrame(
                            timestamp = frame.timestamp,
                            repNumber = frame.repNumber,
                            frameType = KeyFrameType.FORM_ISSUE,
                            imagePath = it,
                            description = "Rep #${frame.repNumber} - Issue: ${frame.formIssues.firstOrNull() ?: "Form break"}\nScore: ${(frame.formScore * 100).toInt()}%",
                            formScore = frame.formScore
                        )
                    )
                    savedFrames.add(frame)
                }
            }

            // 4. Progressive samples throughout workout (early, mid, late)
            // This shows performance progression
            val remainingFrames = capturedFrames.filter { it !in savedFrames }
            if (remainingFrames.isNotEmpty()) {
                val progressiveFrames = listOf(
                    remainingFrames.firstOrNull(),  // Early rep
                    remainingFrames.getOrNull(remainingFrames.size / 3),  // First third
                    remainingFrames.getOrNull(remainingFrames.size / 2),  // Middle
                    remainingFrames.getOrNull(2 * remainingFrames.size / 3),  // Last third
                    remainingFrames.lastOrNull()  // Final rep
                ).filterNotNull().distinct()

                progressiveFrames.forEachIndexed { index, frame ->
                    val savedPath = saveFrameWithOverlay(
                        frame,
                        framesDir,
                        "04_progression_${index + 1}",
                        goodForm = frame.formScore >= 0.7f
                    )
                    savedPath?.let {
                        val position = when (index) {
                            0 -> "Early"
                            progressiveFrames.size - 1 -> "Final"
                            else -> "Mid"
                        }
                        keyFrames.add(
                            KeyFrame(
                                timestamp = frame.timestamp,
                                repNumber = frame.repNumber,
                                frameType = KeyFrameType.PROGRESSION,
                                imagePath = it,
                                description = "$position Rep #${frame.repNumber} - Form Score: ${(frame.formScore * 100).toInt()}%",
                                formScore = frame.formScore
                            )
                        )
                        savedFrames.add(frame)
                    }
                }
            }

            // 5. If we still don't have 10 frames, add more samples
            val MIN_FRAMES = 10
            if (keyFrames.size < MIN_FRAMES) {
                val additionalNeeded = MIN_FRAMES - keyFrames.size
                val additionalFrames = capturedFrames
                    .filter { it !in savedFrames }
                    .sortedByDescending { it.formScore }  // Take best remaining frames
                    .take(additionalNeeded)

                additionalFrames.forEachIndexed { index, frame ->
                    val savedPath = saveFrameWithOverlay(
                        frame,
                        framesDir,
                        "05_additional_${index + 1}",
                        goodForm = frame.formScore >= 0.7f
                    )
                    savedPath?.let {
                        keyFrames.add(
                            KeyFrame(
                                timestamp = frame.timestamp,
                                repNumber = frame.repNumber,
                                frameType = KeyFrameType.REFERENCE,
                                imagePath = it,
                                description = "Rep #${frame.repNumber} - Form Score: ${(frame.formScore * 100).toInt()}%",
                                formScore = frame.formScore
                            )
                        )
                    }
                }
            }

            Log.d(TAG, "Saved ${keyFrames.size} key frames for session $sessionId (${capturedFrames.size} total reps)")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing key frames", e)
        }

        return keyFrames
    }

    /**
     * Save a frame with skeletal overlay
     */
    private fun saveFrameWithOverlay(
        frame: CapturedFrame,
        directory: File,
        fileName: String,
        goodForm: Boolean
    ): String? {
        return try {
            val overlayBitmap = SkeletalOverlayRenderer.drawSkeletonWithAnnotations(
                originalBitmap = frame.bitmap,
                pose = frame.poseData,
                formIssues = frame.formIssues,
                goodForm = goodForm
            )

            val file = File(directory, "$fileName.jpg")
            FileOutputStream(file).use { out ->
                overlayBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            overlayBitmap.recycle()

            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving frame with overlay", e)
            null
        }
    }

    /**
     * Clear captured frames (call after processing)
     */
    fun clear() {
        capturedFrames.forEach { it.bitmap.recycle() }
        capturedFrames.clear()
    }

    /**
     * Get current frame count
     */
    fun getFrameCount(): Int = capturedFrames.size
}
