package org.liftrr.utils

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.liftrr.domain.analytics.ExerciseSpecificMetrics
import org.liftrr.domain.analytics.WorkoutReport
import org.liftrr.domain.workout.KeyFrame
import org.liftrr.domain.workout.KeyFrameType
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
     * Internal PDF export with key frames - Enhanced with Liftrr theme
     */
    private fun exportAsPdfWithFrames(
        context: Context,
        report: WorkoutReport,
        keyFrames: List<KeyFrame>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val pageWidth = pageInfo.pageWidth.toFloat()

        // Liftrr theme colors (from Color.kt)
        val primaryOrange = "#FF6B35".toColorInt()
        val deepBlue = "#1565C0".toColorInt()
        val goodGreen = "#7CB342".toColorInt()
        val badRed = "#FF5252".toColorInt()
        val darkText = android.graphics.Color.parseColor("#1C1B1F")
        val lightGray = "#79747E".toColorInt()

        val paint = Paint()
        Paint().apply {
            textSize = 28f
            isFakeBoldText = true
            color = primaryOrange
        }
        val headerPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = primaryOrange
        }
        val textPaint = Paint().apply {
            textSize = 14f
            color = darkText
        }
        val subtextPaint = Paint().apply {
            textSize = 12f
            color = lightGray
        }

        var yPosition = 50f
        val leftMargin = 40f
        val rightMargin = 40f
        val lineSpacing = 20f

        // Header bar - full width
        paint.color = primaryOrange
        canvas.drawRect(0f, 0f, pageWidth, 80f, paint)

        // Title in white on orange background
        val whitePaint = Paint().apply {
            textSize = 28f
            isFakeBoldText = true
            color = android.graphics.Color.WHITE
        }
        canvas.drawText("LIFTRR", leftMargin, 45f, whitePaint)
        whitePaint.textSize = 14f
        whitePaint.alpha = 230
        canvas.drawText("Workout Performance Report", leftMargin, 65f, whitePaint)

        yPosition = 105f

        // Date and Exercise info
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        canvas.drawText(
            "${
                report.exerciseType.name.replace(
                    "_",
                    " "
                )
            } • ${dateFormat.format(Date())}", leftMargin, yPosition, textPaint
        )
        yPosition += lineSpacing * 1.5f

        // Overall Score Section with colored background
        val scoreCardHeight = 90f
        paint.color = android.graphics.Color.parseColor("#FFF5F2")
        canvas.drawRoundRect(
            leftMargin,
            yPosition,
            pageWidth - rightMargin,
            yPosition + scoreCardHeight,
            12f,
            12f,
            paint
        )

        yPosition += 22f
        canvas.drawText("Overall Performance", leftMargin + 15, yPosition, headerPaint)
        yPosition += lineSpacing + 5f

        // Score with large, bold display
        val scorePaint = Paint().apply {
            textSize = 32f
            isFakeBoldText = true
            color = primaryOrange
        }
        canvas.drawText("${report.overallScore.toInt()}%", leftMargin + 15, yPosition, scorePaint)

        // Grade badge
        val gradePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
            color = when (report.grade) {
                "A" -> goodGreen
                "B" -> deepBlue
                "C" -> android.graphics.Color.parseColor("#FF9800")
                else -> badRed
            }
        }
        canvas.drawText(report.grade, leftMargin + 100, yPosition, gradePaint)

        yPosition += lineSpacing
        canvas.drawText(
            "${report.totalReps} reps: ${report.goodReps} good • ${report.badReps} needs work",
            leftMargin + 15,
            yPosition,
            subtextPaint
        )

        yPosition += scoreCardHeight - 42f // Move past the card
        yPosition += lineSpacing

        // Key Metrics Section with cards
        canvas.drawText("Key Metrics", leftMargin, yPosition, headerPaint)
        yPosition += lineSpacing + 5f

        // Create metric cards in 2x2 grid
        val cardWidth = 240f
        val cardHeight = 65f
        val cardGap = 15f
        val cardBg = android.graphics.Color.parseColor("#F4F4F8")

        val metricPaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
            color = primaryOrange
        }

        // Helper function to safely format percentages
        fun safePercent(value: Float): String {
            return if (value.isNaN() || value.isInfinite()) "N/A" else "${value.toInt()}%"
        }

        // Card 1: ROM
        paint.color = cardBg
        canvas.drawRoundRect(
            leftMargin,
            yPosition,
            leftMargin + cardWidth,
            yPosition + cardHeight,
            8f,
            8f,
            paint
        )
        canvas.drawText("Range of Motion", leftMargin + 12, yPosition + 20, subtextPaint)
        canvas.drawText(
            safePercent(report.rangeOfMotion.consistency),
            leftMargin + 12,
            yPosition + 48,
            metricPaint
        )

        // Card 2: Symmetry
        canvas.drawRoundRect(
            leftMargin + cardWidth + cardGap,
            yPosition,
            leftMargin + 2 * cardWidth + cardGap,
            yPosition + cardHeight,
            8f,
            8f,
            paint
        )
        canvas.drawText(
            "Symmetry",
            leftMargin + cardWidth + cardGap + 12,
            yPosition + 20,
            subtextPaint
        )
        canvas.drawText(
            safePercent(report.symmetry.overallSymmetry),
            leftMargin + cardWidth + cardGap + 12,
            yPosition + 48,
            metricPaint
        )

        yPosition += cardHeight + cardGap

        // Card 3: Form Consistency
        canvas.drawRoundRect(
            leftMargin,
            yPosition,
            leftMargin + cardWidth,
            yPosition + cardHeight,
            8f,
            8f,
            paint
        )
        canvas.drawText("Form Consistency", leftMargin + 12, yPosition + 20, subtextPaint)
        canvas.drawText(
            safePercent(report.formConsistency.consistencyScore),
            leftMargin + 12,
            yPosition + 48,
            metricPaint
        )

        // Card 4: Quality Trend
        canvas.drawRoundRect(
            leftMargin + cardWidth + cardGap,
            yPosition,
            leftMargin + 2 * cardWidth + cardGap,
            yPosition + cardHeight,
            8f,
            8f,
            paint
        )
        canvas.drawText(
            "Quality Trend",
            leftMargin + cardWidth + cardGap + 12,
            yPosition + 20,
            subtextPaint
        )
        val trendPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = when (report.formConsistency.qualityTrend.lowercase()) {
                "improving" -> goodGreen
                "declining" -> badRed
                else -> lightGray
            }
        }
        canvas.drawText(
            report.formConsistency.qualityTrend,
            leftMargin + cardWidth + cardGap + 12,
            yPosition + 48,
            trendPaint
        )

        yPosition += cardHeight + lineSpacing * 1.5f

        // Exercise-Specific Metrics
        report.exerciseSpecificMetrics?.let { metrics ->
            canvas.drawText("Exercise Analysis", leftMargin, yPosition, headerPaint)
            yPosition += lineSpacing + 5f

            when (metrics) {
                is ExerciseSpecificMetrics.SquatMetrics -> {
                    canvas.drawText(
                        "• Average Depth: ${safePercent(metrics.averageDepth)}",
                        leftMargin + 12,
                        yPosition,
                        textPaint
                    )
                    yPosition += lineSpacing
                    canvas.drawText(
                        "• Knee Alignment: ${safePercent(metrics.kneeTracking.kneeAlignment)}",
                        leftMargin + 12,
                        yPosition,
                        textPaint
                    )
                    yPosition += lineSpacing
                    canvas.drawText(
                        "• Hip Mobility: ${safePercent(metrics.hipMobility)}",
                        leftMargin + 12,
                        yPosition,
                        textPaint
                    )
                    yPosition += lineSpacing
                }

                is ExerciseSpecificMetrics.DeadliftMetrics -> {
                    canvas.drawText(
                        "• Hip Hinge Quality: ${safePercent(metrics.hipHingeQuality)}",
                        leftMargin + 12,
                        yPosition,
                        textPaint
                    )
                    yPosition += lineSpacing
                    canvas.drawText(
                        "• Spine Neutral: ${safePercent(metrics.backStraightness.spineNeutral)}",
                        leftMargin + 12,
                        yPosition,
                        textPaint
                    )
                    yPosition += lineSpacing
                    canvas.drawText(
                        "• Lockout: ${safePercent(metrics.lockoutCompletion)}",
                        leftMargin + 12,
                        yPosition,
                        textPaint
                    )
                    yPosition += lineSpacing
                }

                is ExerciseSpecificMetrics.BenchPressMetrics -> {
                    canvas.drawText(
                        "• Bottom Angle: ${metrics.elbowAngle.bottomAngle.toInt()}°",
                        leftMargin + 12,
                        yPosition,
                        textPaint
                    )
                    yPosition += lineSpacing
                    canvas.drawText(
                        "• Elbow Tucking: ${safePercent(metrics.elbowAngle.tucking)}",
                        leftMargin + 12,
                        yPosition,
                        textPaint
                    )
                    yPosition += lineSpacing
                    canvas.drawText(
                        "• Shoulder Stability: ${safePercent(metrics.shoulderPosition.stability)}",
                        leftMargin + 12,
                        yPosition,
                        textPaint
                    )
                    yPosition += lineSpacing
                }
            }
            yPosition += lineSpacing * 0.5f
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
        canvas.drawText(
            "Generated by Liftrr - Your AI Fitness Coach",
            leftMargin,
            yPosition,
            footerPaint
        )

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
            appendLine(
                "Date: ${
                    SimpleDateFormat(
                        "MMMM dd, yyyy 'at' hh:mm a",
                        Locale.getDefault()
                    ).format(Date())
                }"
            )
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
                context, "${context.packageName}.fileprovider", file
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
            appendLine(
                "Date: ${
                    SimpleDateFormat(
                        "MMMM dd, yyyy 'at' hh:mm a",
                        Locale.getDefault()
                    ).format(Date())
                }"
            )
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
     * Add pages with side-by-side phase comparison of good vs bad form
     */
    private fun addKeyFramesPage(
        pdfDocument: PdfDocument,
        keyFrames: List<KeyFrame>,
        headerPaint: Paint,
        textPaint: Paint,
        leftMargin: Float
    ) {
        if (keyFrames.isEmpty()) return

        // Group frames by movement phase
        keyFrames.filter { it.movementPhase != null }.groupBy { it.movementPhase!! }

        // Find best and worst rep numbers
        val bestRepFrames = keyFrames.filter { it.frameType == KeyFrameType.BEST_REP }
        val worstRepFrames = keyFrames.filter { it.frameType == KeyFrameType.WORST_REP }

        if (bestRepFrames.isEmpty() || worstRepFrames.isEmpty()) {
            // Fallback to old behavior if no phase-based frames
            addLegacyKeyFramesPage(pdfDocument, keyFrames, headerPaint, textPaint, leftMargin)
            return
        }

        val bestRepNumber = bestRepFrames.firstOrNull()?.repNumber
        val worstRepNumber = worstRepFrames.firstOrNull()?.repNumber

        var pageNumber = 2
        val phaseOrder = listOf(
            org.liftrr.domain.workout.MovementPhase.SETUP,
            org.liftrr.domain.workout.MovementPhase.DESCENT,
            org.liftrr.domain.workout.MovementPhase.BOTTOM,
            org.liftrr.domain.workout.MovementPhase.ASCENT,
            org.liftrr.domain.workout.MovementPhase.LOCKOUT
        )

        // Create pages with side-by-side comparisons (2 phases per page)
        phaseOrder.chunked(2).forEach { phaseChunk ->
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var yPosition = 50f
            val lineSpacing = 18f

            // Title on first page
            if (pageNumber == 3) {
                canvas.drawText(
                    "Form Comparison: Best vs Worst Rep",
                    leftMargin,
                    yPosition,
                    headerPaint
                )
                yPosition += lineSpacing * 2
            }

            phaseChunk.forEach { phase ->
                val phaseName = phase.name.lowercase().replaceFirstChar { it.uppercase() }

                // Get frames for this phase from best and worst reps
                val goodFrame =
                    keyFrames.find { it.repNumber == bestRepNumber && it.movementPhase == phase }
                val badFrame =
                    keyFrames.find { it.repNumber == worstRepNumber && it.movementPhase == phase }

                if (goodFrame != null || badFrame != null) {
                    // Phase header
                    canvas.drawText("$phaseName Phase", leftMargin, yPosition, headerPaint)
                    yPosition += lineSpacing * 1.5f

                    val imageWidth = 250f
                    val imageHeight = 140f
                    val diagPaint = Paint().apply {
                        textSize = 9f
                    }

                    // Draw good form on left
                    goodFrame?.let { frame ->
                        try {
                            val bitmap = BitmapFactory.decodeFile(frame.imagePath)
                            if (bitmap != null) {
                                val destRect = android.graphics.RectF(
                                    leftMargin,
                                    yPosition,
                                    leftMargin + imageWidth,
                                    yPosition + imageHeight
                                )
                                canvas.drawBitmap(bitmap, null, destRect, null)
                                bitmap.recycle()

                                // Good form label
                                var labelY = yPosition + imageHeight + 15f
                                val goodPaint = Paint().apply {
                                    textSize = 14f
                                    color = android.graphics.Color.parseColor("#2E7D32") // Green
                                    isFakeBoldText = true
                                }
                                canvas.drawText("✓ Good Form", leftMargin, labelY, goodPaint)
                                labelY += 14f

                                // Display form score
                                diagPaint.color = android.graphics.Color.parseColor("#2E7D32")
                                val scoreText = "Score: ${(frame.formScore ?: 0f * 100).toInt()}%"
                                canvas.drawText(scoreText, leftMargin, labelY, diagPaint)
                                labelY += 11f

                                // Display diagnostics or key angles
                                if (frame.diagnostics.isNotEmpty()) {
                                    frame.diagnostics.take(3).forEach { diagnostic ->
                                        canvas.drawText(
                                            "• ${diagnostic.angle}: ${diagnostic.measured.toInt()}°",
                                            leftMargin,
                                            labelY,
                                            diagPaint
                                        )
                                        labelY += 11f
                                        canvas.drawText(
                                            "  ${diagnostic.expected}",
                                            leftMargin,
                                            labelY,
                                            diagPaint
                                        )
                                        labelY += 11f
                                    }
                                } else {
                                    canvas.drawText(
                                        "All angles within range",
                                        leftMargin,
                                        labelY,
                                        diagPaint
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "WorkoutReportExporter",
                                "Error loading good form image",
                                e
                            )
                        }
                    }

                    // Draw bad form on right
                    badFrame?.let { frame ->
                        try {
                            val bitmap = BitmapFactory.decodeFile(frame.imagePath)
                            if (bitmap != null) {
                                val rightX = leftMargin + imageWidth + 15f
                                val destRect = android.graphics.RectF(
                                    rightX, yPosition, rightX + imageWidth, yPosition + imageHeight
                                )
                                canvas.drawBitmap(bitmap, null, destRect, null)
                                bitmap.recycle()

                                // Bad form label
                                var labelY = yPosition + imageHeight + 15f
                                val badPaint = Paint().apply {
                                    textSize = 14f
                                    color = android.graphics.Color.parseColor("#D32F2F") // Red
                                    isFakeBoldText = true
                                }
                                canvas.drawText("✗ Needs Work", rightX, labelY, badPaint)
                                labelY += 14f

                                // Display form score
                                diagPaint.color = android.graphics.Color.parseColor("#D32F2F")
                                val scoreText = "Score: ${(frame.formScore ?: 0f * 100).toInt()}%"
                                canvas.drawText(scoreText, rightX, labelY, diagPaint)
                                labelY += 11f

                                // Display detailed diagnostics with reasoning
                                if (frame.diagnostics.isNotEmpty()) {
                                    frame.diagnostics.take(3).forEach { diagnostic ->
                                        // Issue description
                                        canvas.drawText(
                                            "• ${diagnostic.issue}",
                                            rightX,
                                            labelY,
                                            diagPaint
                                        )
                                        labelY += 11f
                                        // Measured angle
                                        canvas.drawText(
                                            "  ${diagnostic.angle}: ${diagnostic.measured.toInt()}°",
                                            rightX,
                                            labelY,
                                            diagPaint
                                        )
                                        labelY += 11f
                                        // Expected range
                                        canvas.drawText(
                                            "  Expected: ${diagnostic.expected}",
                                            rightX,
                                            labelY,
                                            diagPaint
                                        )
                                        labelY += 11f
                                    }
                                } else {
                                    canvas.drawText(
                                        "Low form score - check technique",
                                        rightX,
                                        labelY,
                                        diagPaint
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "WorkoutReportExporter",
                                "Error loading bad form image",
                                e
                            )
                        }
                    }

                    yPosition += imageHeight + 80f // More space for diagnostics
                }
            }

            // Footer
            val footerPaint = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.GRAY
            }
            canvas.drawText(
                "Page ${pageNumber - 1} - Movement Analysis",
                leftMargin,
                820f,
                footerPaint
            )

            pdfDocument.finishPage(page)
        }
    }

    /**
     * Legacy key frames display for backward compatibility
     */
    private fun addLegacyKeyFramesPage(
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

        canvas.drawText("Key Frames Analysis", leftMargin, yPosition, headerPaint)
        yPosition += lineSpacing * 2

        keyFrames.take(3).forEach { keyFrame ->
            try {
                val bitmap = BitmapFactory.decodeFile(keyFrame.imagePath)
                if (bitmap != null) {
                    val maxWidth = 515f
                    val maxHeight = 200f
                    val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height)
                    val scaledWidth = bitmap.width * scale
                    val scaledHeight = bitmap.height * scale

                    val destRect = android.graphics.RectF(
                        leftMargin, yPosition, leftMargin + scaledWidth, yPosition + scaledHeight
                    )
                    canvas.drawBitmap(bitmap, null, destRect, null)
                    bitmap.recycle()

                    yPosition += scaledHeight + lineSpacing

                    val descLines = keyFrame.description.split("\n")
                    descLines.forEach { line ->
                        canvas.drawText(line, leftMargin, yPosition, textPaint)
                        yPosition += lineSpacing
                    }

                    yPosition += lineSpacing
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkoutReportExporter", "Error loading key frame image", e)
            }
        }

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
