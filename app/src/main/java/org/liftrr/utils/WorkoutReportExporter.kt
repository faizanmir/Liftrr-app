package org.liftrr.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.liftrr.domain.analytics.ExerciseSpecificMetrics
import org.liftrr.domain.analytics.WorkoutReport
import org.liftrr.domain.workout.KeyFrame
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting and sharing workout reports in various formats
 */
object WorkoutReportExporter {

    /**
     * Export workout report as PDF with key frames
     */
    fun exportAsPdf(context: Context, report: WorkoutReport, keyFramesJson: String? = null): File {
        val keyFrames = keyFramesJson?.let {
            try {
                val type = object : TypeToken<List<KeyFrame>>() {}.type
                Gson().fromJson<List<KeyFrame>>(it, type)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()

        return exportAsPdfWithFrames(context, report, keyFrames)
    }

    /**
     * Internal PDF export with key frames
     */
    private fun exportAsPdfWithFrames(context: Context, report: WorkoutReport, keyFrames: List<KeyFrame>): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 14f
        }

        var yPosition = 50f
        val leftMargin = 40f
        val lineSpacing = 25f

        // Title
        canvas.drawText("Liftrr Workout Report", leftMargin, yPosition, titlePaint)
        yPosition += lineSpacing * 2

        // Date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        canvas.drawText("Date: ${dateFormat.format(Date(report.durationMs))}", leftMargin, yPosition, textPaint)
        yPosition += lineSpacing

        // Exercise Type
        canvas.drawText("Exercise: ${report.exerciseType.name.replace("_", " ")}", leftMargin, yPosition, textPaint)
        yPosition += lineSpacing * 2

        // Overall Score Section
        canvas.drawText("Overall Performance", leftMargin, yPosition, headerPaint)
        yPosition += lineSpacing
        canvas.drawText("Score: ${report.overallScore.toInt()}% (Grade: ${report.grade})", leftMargin + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Total Reps: ${report.totalReps} (${report.goodReps} good, ${report.badReps} bad)", leftMargin + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Duration: ${formatDuration(report.durationMs)}", leftMargin + 20, yPosition, textPaint)
        yPosition += lineSpacing * 2

        // Key Metrics Section
        canvas.drawText("Key Metrics", leftMargin, yPosition, headerPaint)
        yPosition += lineSpacing
        canvas.drawText("Range of Motion Consistency: ${report.rangeOfMotion.consistency.toInt()}%", leftMargin + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Symmetry: ${report.symmetry.overallSymmetry.toInt()}%", leftMargin + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Form Consistency: ${report.formConsistency.consistencyScore.toInt()}%", leftMargin + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Quality Trend: ${report.formConsistency.qualityTrend}", leftMargin + 20, yPosition, textPaint)
        yPosition += lineSpacing * 2

        // Exercise-Specific Metrics
        report.exerciseSpecificMetrics?.let { metrics ->
            canvas.drawText("Exercise-Specific Analysis", leftMargin, yPosition, headerPaint)
            yPosition += lineSpacing

            when (metrics) {
                is ExerciseSpecificMetrics.SquatMetrics -> {
                    canvas.drawText("Average Depth: ${metrics.averageDepth.toInt()}°", leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                    canvas.drawText("Knee Tracking: ${metrics.kneeTracking.kneeAlignment.toInt()}%", leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                    canvas.drawText("Hip Mobility: ${metrics.hipMobility.toInt()}%", leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                }
                is ExerciseSpecificMetrics.DeadliftMetrics -> {
                    canvas.drawText("Hip Hinge Quality: ${metrics.hipHingeQuality.toInt()}%", leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                    canvas.drawText("Back Straightness: ${metrics.backStraightness.spineNeutral.toInt()}%", leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                    canvas.drawText("Lockout Completion: ${metrics.lockoutCompletion.toInt()}%", leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                }
                is ExerciseSpecificMetrics.BenchPressMetrics -> {
                    canvas.drawText("Bottom Elbow Angle: ${metrics.elbowAngle.bottomAngle.toInt()}°", leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                    canvas.drawText("Elbow Tucking: ${metrics.elbowAngle.tucking.toInt()}%", leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                    canvas.drawText("Shoulder Stability: ${metrics.shoulderPosition.stability.toInt()}%", leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                }
            }
            yPosition += lineSpacing
        }

        // Recommendations Section
        if (report.recommendations.isNotEmpty()) {
            canvas.drawText("Recommendations", leftMargin, yPosition, headerPaint)
            yPosition += lineSpacing

            report.recommendations.forEachIndexed { index, recommendation ->
                val bulletText = "${index + 1}. $recommendation"
                // Simple word wrap for long recommendations
                val maxWidth = 500f
                val words = bulletText.split(" ")
                var line = ""

                words.forEach { word ->
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    val textWidth = textPaint.measureText(testLine)

                    if (textWidth > maxWidth && line.isNotEmpty()) {
                        canvas.drawText(line, leftMargin + 20, yPosition, textPaint)
                        yPosition += lineSpacing
                        line = word
                    } else {
                        line = testLine
                    }
                }

                if (line.isNotEmpty()) {
                    canvas.drawText(line, leftMargin + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                }
            }
        }

        // Footer
        yPosition = 800f
        val footerPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.GRAY
        }
        canvas.drawText("Generated by Liftrr - Your AI Fitness Coach", leftMargin, yPosition, footerPaint)

        pdfDocument.finishPage(page)

        // Add key frames page if available
        if (keyFrames.isNotEmpty()) {
            addKeyFramesPage(pdfDocument, keyFrames, headerPaint, textPaint, leftMargin)
        }

        // Save PDF to cache directory
        val fileName = "Liftrr_${report.exerciseType.name}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        pdfDocument.close()

        return file
    }

    /**
     * Export workout report as plain text
     */
    fun exportAsText(report: WorkoutReport): String {
        return buildString {
            appendLine("=== LIFTRR WORKOUT REPORT ===")
            appendLine()
            appendLine("Exercise: ${report.exerciseType.name.replace("_", " ")}")
            appendLine("Date: ${SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())}")
            appendLine()
            appendLine("OVERALL PERFORMANCE")
            appendLine("Score: ${report.overallScore.toInt()}% (Grade: ${report.grade})")
            appendLine("Total Reps: ${report.totalReps} (${report.goodReps} good, ${report.badReps} bad)")
            appendLine("Duration: ${formatDuration(report.durationMs)}")
            appendLine()
            appendLine("KEY METRICS")
            appendLine("Range of Motion Consistency: ${report.rangeOfMotion.consistency.toInt()}%")
            appendLine("Symmetry: ${report.symmetry.overallSymmetry.toInt()}%")
            appendLine("Form Consistency: ${report.formConsistency.consistencyScore.toInt()}%")
            appendLine("Quality Trend: ${report.formConsistency.qualityTrend}")
            appendLine()

            report.exerciseSpecificMetrics?.let { metrics ->
                appendLine("EXERCISE-SPECIFIC ANALYSIS")
                when (metrics) {
                    is ExerciseSpecificMetrics.SquatMetrics -> {
                        appendLine("Average Depth: ${metrics.averageDepth.toInt()}°")
                        appendLine("Knee Tracking: ${metrics.kneeTracking.kneeAlignment.toInt()}%")
                        appendLine("Hip Mobility: ${metrics.hipMobility.toInt()}%")
                    }
                    is ExerciseSpecificMetrics.DeadliftMetrics -> {
                        appendLine("Hip Hinge Quality: ${metrics.hipHingeQuality.toInt()}%")
                        appendLine("Back Straightness: ${metrics.backStraightness.spineNeutral.toInt()}%")
                        appendLine("Lockout Completion: ${metrics.lockoutCompletion.toInt()}%")
                    }
                    is ExerciseSpecificMetrics.BenchPressMetrics -> {
                        appendLine("Bottom Elbow Angle: ${metrics.elbowAngle.bottomAngle.toInt()}°")
                        appendLine("Elbow Tucking: ${metrics.elbowAngle.tucking.toInt()}%")
                        appendLine("Shoulder Stability: ${metrics.shoulderPosition.stability.toInt()}%")
                    }
                }
                appendLine()
            }

            if (report.recommendations.isNotEmpty()) {
                appendLine("RECOMMENDATIONS")
                report.recommendations.forEachIndexed { index, rec ->
                    appendLine("${index + 1}. $rec")
                }
                appendLine()
            }

            appendLine("Generated by Liftrr - Your AI Fitness Coach")
        }
    }

    /**
     * Share report via Android share sheet
     */
    fun shareReport(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "My Liftrr Workout Report")
                putExtra(Intent.EXTRA_TEXT, "Check out my workout results from Liftrr!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Workout Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            android.util.Log.e("WorkoutReportExporter", "Failed to share report", e)
            e.printStackTrace()
        }
    }

    /**
     * Share report as text
     */
    fun shareAsText(context: Context, text: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "My Liftrr Workout Report")
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Workout Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            android.util.Log.e("WorkoutReportExporter", "Failed to share text", e)
            e.printStackTrace()
        }
    }

    /**
     * Export simple workout summary (for playback screen with limited data)
     */
    fun exportSimpleSummaryAsText(
        exerciseName: String,
        totalReps: Int,
        goodReps: Int,
        badReps: Int,
        overallScore: Float,
        grade: String,
        durationMs: Long
    ): String {
        return buildString {
            appendLine("=== LIFTRR WORKOUT REPORT ===")
            appendLine()
            appendLine("Exercise: $exerciseName")
            appendLine("Date: ${SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())}")
            appendLine()
            appendLine("PERFORMANCE SUMMARY")
            appendLine("Score: ${overallScore.toInt()}% (Grade: $grade)")
            appendLine("Total Reps: $totalReps ($goodReps good, $badReps bad)")
            appendLine("Duration: ${formatDuration(durationMs)}")
            appendLine()
            appendLine("Generated by Liftrr - Your AI Fitness Coach")
        }
    }

    /**
     * Add a page with key frame images
     */
    private fun addKeyFramesPage(
        pdfDocument: PdfDocument,
        keyFrames: List<KeyFrame>,
        headerPaint: Paint,
        textPaint: Paint,
        leftMargin: Float
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var yPosition = 50f
        val lineSpacing = 20f

        // Title
        canvas.drawText("Key Frames Analysis", leftMargin, yPosition, headerPaint)
        yPosition += lineSpacing * 2

        // Display each key frame
        keyFrames.take(3).forEach { keyFrame ->  // Limit to 3 frames per page
            try {
                val bitmap = BitmapFactory.decodeFile(keyFrame.imagePath)
                if (bitmap != null) {
                    // Scale bitmap to fit page width
                    val maxWidth = 515f  // Page width minus margins
                    val maxHeight = 200f
                    val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height)
                    val scaledWidth = bitmap.width * scale
                    val scaledHeight = bitmap.height * scale

                    // Draw bitmap
                    val destRect = android.graphics.RectF(
                        leftMargin,
                        yPosition,
                        leftMargin + scaledWidth,
                        yPosition + scaledHeight
                    )
                    canvas.drawBitmap(bitmap, null, destRect, null)
                    bitmap.recycle()

                    yPosition += scaledHeight + lineSpacing

                    // Draw description
                    val descLines = keyFrame.description.split("\n")
                    descLines.forEach { line ->
                        canvas.drawText(line, leftMargin, yPosition, textPaint)
                        yPosition += lineSpacing
                    }

                    yPosition += lineSpacing  // Extra space between frames
                }
            } catch (e: Exception) {
                // Skip frame if image can't be loaded
                android.util.Log.e("WorkoutReportExporter", "Error loading key frame image", e)
            }
        }

        // Footer
        val footerPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.GRAY
        }
        canvas.drawText("Page 2 - Key Frames", leftMargin, 820f, footerPaint)

        pdfDocument.finishPage(page)
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${remainingSeconds}s"
        }
    }
}
