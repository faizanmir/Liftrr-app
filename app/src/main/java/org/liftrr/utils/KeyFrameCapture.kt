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
     * Capture a frame during workout with phase information
     */
    fun captureFrame(
        bitmap: Bitmap,
        timestamp: Long,
        repNumber: Int,
        poseData: PoseDetectionResult.Success,
        formScore: Float,
        formIssues: List<String>,
        movementPhase: org.liftrr.domain.workout.MovementPhase = org.liftrr.domain.workout.MovementPhase.LOCKOUT,
        diagnostics: List<org.liftrr.domain.workout.FormDiagnostic> = emptyList()
    ) {
        capturedFrames.add(
            CapturedFrame(
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false),
                timestamp = timestamp,
                repNumber = repNumber,
                poseData = poseData,
                formScore = formScore,
                formIssues = formIssues,
                movementPhase = movementPhase,
                diagnostics = diagnostics
            )
        )
    }

    /**
     * Process and save key frames at end of workout
     * Captures multiple phases per rep for best/worst/medium reps
     * Returns list of KeyFrame metadata with minimum 10 frames for quality analysis
     */
    fun processAndSaveKeyFrames(sessionId: String): List<KeyFrame> {
        if (capturedFrames.isEmpty()) {
            Log.w(TAG, "No frames captured")
            return emptyList()
        }

        val keyFrames = mutableListOf<KeyFrame>()

        try {
            // Create directory for this session's frames
            val framesDir = File(context.filesDir, "workout_frames/$sessionId")
            if (!framesDir.exists()) {
                framesDir.mkdirs()
            }

            // Group frames by rep number
            val framesByRep = capturedFrames.groupBy { it.repNumber }

            // Calculate average form score per rep
            val repScores = framesByRep.mapValues { (_, frames) ->
                frames.map { it.formScore }.average().toFloat()
            }

            // Select reps for detailed analysis: best, worst, and medium
            val sortedReps = repScores.entries.sortedByDescending { it.value }
            val bestRepNumber = sortedReps.firstOrNull()?.key
            val worstRepNumber = sortedReps.lastOrNull()?.key
            val mediumRepNumber = sortedReps.getOrNull(sortedReps.size / 2)?.key

            val selectedReps = setOfNotNull(bestRepNumber, worstRepNumber, mediumRepNumber)

            // Save all phases for selected reps (best, worst, medium)
            selectedReps.forEach { repNumber ->
                val repFrames = framesByRep[repNumber] ?: return@forEach
                val avgScore = repScores[repNumber] ?: 0f
                val isGoodForm = avgScore >= 0.7f

                val frameType = when (repNumber) {
                    bestRepNumber -> KeyFrameType.BEST_REP
                    worstRepNumber -> KeyFrameType.WORST_REP
                    else -> KeyFrameType.REFERENCE
                }

                val prefix = when (repNumber) {
                    bestRepNumber -> "01_best"
                    worstRepNumber -> "02_worst"
                    else -> "03_medium"
                }

                // Save each phase for this rep
                val phaseOrder = listOf(
                    org.liftrr.domain.workout.MovementPhase.SETUP,
                    org.liftrr.domain.workout.MovementPhase.DESCENT,
                    org.liftrr.domain.workout.MovementPhase.BOTTOM,
                    org.liftrr.domain.workout.MovementPhase.ASCENT,
                    org.liftrr.domain.workout.MovementPhase.LOCKOUT
                )

                phaseOrder.forEach { phase ->
                    // Find the best frame for this phase
                    val phaseFrame = repFrames.filter { it.movementPhase == phase }
                        .maxByOrNull { it.formScore }

                    phaseFrame?.let { frame ->
                        val phaseName = phase.name.lowercase().replaceFirstChar { it.uppercase() }
                        val savedPath = saveFrameWithOverlay(
                            frame,
                            framesDir,
                            "${prefix}_rep${repNumber}_${phase.name.lowercase()}",
                            goodForm = isGoodForm
                        )

                        savedPath?.let {
                            keyFrames.add(
                                KeyFrame(
                                    timestamp = frame.timestamp,
                                    repNumber = repNumber,
                                    frameType = frameType,
                                    imagePath = it,
                                    description = "$phaseName Phase - Rep #$repNumber - Score: ${(frame.formScore * 100).toInt()}%",
                                    formScore = frame.formScore,
                                    movementPhase = phase,
                                    diagnostics = frame.diagnostics
                                )
                            )
                        }
                    }
                }
            }

            // If we don't have enough frames (minimum 10), add some single-phase frames from other reps
            val MIN_FRAMES = 10
            if (keyFrames.size < MIN_FRAMES) {
                val additionalNeeded = MIN_FRAMES - keyFrames.size
                val remainingReps = framesByRep.keys.filterNot { it in selectedReps }

                remainingReps.take(additionalNeeded).forEach { repNumber ->
                    val frames = framesByRep[repNumber] ?: return@forEach
                    val bestFrame = frames.maxByOrNull { it.formScore } ?: return@forEach

                    val savedPath = saveFrameWithOverlay(
                        bestFrame,
                        framesDir,
                        "04_additional_rep${repNumber}",
                        goodForm = bestFrame.formScore >= 0.7f
                    )

                    savedPath?.let {
                        keyFrames.add(
                            KeyFrame(
                                timestamp = bestFrame.timestamp,
                                repNumber = repNumber,
                                frameType = KeyFrameType.REFERENCE,
                                imagePath = it,
                                description = "Rep #$repNumber - ${bestFrame.movementPhase.name.lowercase().replaceFirstChar { it.uppercase() }} - Score: ${(bestFrame.formScore * 100).toInt()}%",
                                formScore = bestFrame.formScore,
                                movementPhase = bestFrame.movementPhase,
                                diagnostics = bestFrame.diagnostics
                            )
                        )
                    }
                }
            }

            Log.d(TAG, "Saved ${keyFrames.size} key frames for session $sessionId from ${framesByRep.size} reps with ${capturedFrames.size} total frames")

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
