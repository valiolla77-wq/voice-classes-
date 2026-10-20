package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.RecordingItem
import com.example.util.ScheduleHelper
import java.io.File
import java.io.FileOutputStream

object PdfExportEngine {
    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN_X = 40
    private const val MARGIN_TOP = 50
    private const val MARGIN_BOTTOM = 50

    /**
     * Generates a beautifully formatted PDF document for a lecture recording transcription.
     */
    fun generatePdf(context: Context, recording: RecordingItem): File? {
        try {
            val pdfDir = File(context.cacheDir, "pdfs")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val sanitizedTitle = recording.title
                .replace("/", "_")
                .replace("\\", "_")
                .replace(":", "_")
                .replace(" ", "_")
                .take(40)
            val pdfFile = File(pdfDir, "${sanitizedTitle}_${recording.id}.pdf")

            val document = PdfDocument()

            val contentWidth = PAGE_WIDTH - (MARGIN_X * 2)

            // Paints
            val titlePaint = TextPaint().apply {
                color = Color.rgb(30, 27, 75) // #1E1B4B
                textSize = 17f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val metaPaint = TextPaint().apply {
                color = Color.rgb(71, 85, 105) // Slate
                textSize = 11f
                isAntiAlias = true
            }

            val bodyPaint = TextPaint().apply {
                color = Color.rgb(15, 23, 42) // #0F172A
                textSize = 13f
                isAntiAlias = true
            }

            val footerPaint = TextPaint().apply {
                color = Color.rgb(148, 163, 184)
                textSize = 9f
                isAntiAlias = true
            }

            val accentPaint = Paint().apply {
                color = Color.rgb(79, 70, 229) // Indigo
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }

            val bannerPaint = Paint().apply {
                color = Color.rgb(241, 245, 249) // Soft slate fill
                style = Paint.Style.FILL
            }

            val bodyText = if (recording.transcription.isNotBlank()) {
                recording.transcription
            } else {
                "هنوز متنی برای این جلسه پیاده‌سازی نشده است یا در حال پردازش می‌باشد."
            }

            // Create StaticLayout for body text
            val bodyLayout = StaticLayout.Builder.obtain(
                bodyText,
                0,
                bodyText.length,
                bodyPaint,
                contentWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.ANYRTL_LTR)
                .setLineSpacing(5f, 1.25f)
                .build()

            var currentLine = 0
            val totalLines = bodyLayout.lineCount
            var pageNumber = 1

            while (currentLine < totalLines || pageNumber == 1) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                var currentY = MARGIN_TOP.toFloat()

                // On Page 1, draw decorative header
                if (pageNumber == 1) {
                    // Header card background
                    val headerRect = RectF(
                        MARGIN_X.toFloat(),
                        currentY,
                        (PAGE_WIDTH - MARGIN_X).toFloat(),
                        currentY + 70f
                    )
                    canvas.drawRoundRect(headerRect, 8f, 8f, bannerPaint)

                    // Left accent bar
                    canvas.drawLine(
                        (PAGE_WIDTH - MARGIN_X).toFloat(),
                        currentY,
                        (PAGE_WIDTH - MARGIN_X).toFloat(),
                        currentY + 70f,
                        accentPaint.apply { strokeWidth = 5f }
                    )

                    // Draw Title
                    val titleLayout = StaticLayout.Builder.obtain(
                        recording.title,
                        0,
                        recording.title.length,
                        titlePaint,
                        contentWidth - 20
                    )
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setTextDirection(TextDirectionHeuristics.ANYRTL_LTR)
                        .build()

                    canvas.save()
                    canvas.translate(MARGIN_X + 10f, currentY + 12f)
                    titleLayout.draw(canvas)
                    canvas.restore()

                    // Subtitle / Meta info
                    val dateStr = ScheduleHelper.formatDate(recording.createdAt)
                    val durStr = ScheduleHelper.formatDuration(recording.durationSeconds)
                    val metaText = "تاریخ: $dateStr  |  مدت صوت: $durStr  |  درس: ${recording.courseName}"

                    val metaLayout = StaticLayout.Builder.obtain(
                        metaText,
                        0,
                        metaText.length,
                        metaPaint,
                        contentWidth - 20
                    )
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setTextDirection(TextDirectionHeuristics.ANYRTL_LTR)
                        .build()

                    canvas.save()
                    canvas.translate(MARGIN_X + 10f, currentY + 45f)
                    metaLayout.draw(canvas)
                    canvas.restore()

                    currentY += 90f
                } else {
                    // Subsequent page mini-header
                    canvas.drawText(recording.title, MARGIN_X.toFloat(), currentY, metaPaint)
                    canvas.drawLine(
                        MARGIN_X.toFloat(),
                        currentY + 5f,
                        (PAGE_WIDTH - MARGIN_X).toFloat(),
                        currentY + 5f,
                        accentPaint.apply { strokeWidth = 0.5f }
                    )
                    currentY += 25f
                }

                // Available height on this page for text
                val maxY = PAGE_HEIGHT - MARGIN_BOTTOM - 20f

                // Draw lines that fit on this page
                val startLine = currentLine
                var endLine = currentLine
                val startLineTop = if (startLine < totalLines) bodyLayout.getLineTop(startLine) else 0

                while (endLine < totalLines) {
                    val lineBottom = bodyLayout.getLineBottom(endLine)
                    val heightNeeded = (lineBottom - startLineTop)
                    if (currentY + heightNeeded > maxY) {
                        break
                    }
                    endLine++
                }

                if (startLine < totalLines) {
                    canvas.save()
                    // Clip rect for safety
                    canvas.clipRect(
                        MARGIN_X.toFloat(),
                        currentY,
                        (PAGE_WIDTH - MARGIN_X).toFloat(),
                        maxY
                    )
                    // Offset translation so startLine is drawn at currentY
                    canvas.translate(MARGIN_X.toFloat(), currentY - startLineTop)
                    bodyLayout.draw(canvas)
                    canvas.restore()

                    currentLine = if (endLine > startLine) endLine else startLine + 1
                }

                // Footer
                val footerText = "ClassVoice  •  صفحه $pageNumber"
                canvas.drawText(
                    footerText,
                    (PAGE_WIDTH / 2f) - (footerPaint.measureText(footerText) / 2f),
                    (PAGE_HEIGHT - MARGIN_BOTTOM / 2f),
                    footerPaint
                )

                document.finishPage(page)
                pageNumber++

                // Safety break for extremely large files
                if (pageNumber > 50) break
            }

            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Creates an Android Share Intent for the generated PDF.
     */
    fun createShareIntent(context: Context, pdfFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Creates an Intent to view/open the PDF in an external reader.
     */
    fun createViewIntent(context: Context, pdfFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
