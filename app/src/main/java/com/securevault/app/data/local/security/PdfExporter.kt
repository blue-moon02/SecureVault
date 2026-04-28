package com.securevault.app.data.local.security

import android.content.Context
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.EncryptionConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.UnitValue
import com.securevault.app.domain.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Exports a list of notes to a password-protected PDF in the app's private exports dir.
     *
     * @param notes        Notes to export (sensitive notes filtered by caller)
     * @param userPassword Password set by the user to open the PDF
     * @param ownerPassword Internal password to restrict editing (auto-generated)
     * @return The exported File, or null on failure
     */
    fun exportNotes(
        notes: List<Note>,
        userPassword: String,
        ownerPassword: String = userPassword + "_owner_${System.currentTimeMillis()}"
    ): File? = runCatching {
        val exportsDir = File(context.filesDir, "exports").also { it.mkdirs() }
        val file = File(exportsDir, "securevault_export_${System.currentTimeMillis()}.pdf")

        // Encrypt with AES-256 + user/owner passwords
        val writerProps = WriterProperties()
            .setStandardEncryption(
                userPassword.toByteArray(Charsets.UTF_8),
                ownerPassword.toByteArray(Charsets.UTF_8),
                EncryptionConstants.ALLOW_PRINTING or EncryptionConstants.ALLOW_COPY,
                EncryptionConstants.ENCRYPTION_AES_256
            )

        PdfWriter(file, writerProps).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc, PageSize.A4).use { doc ->
                    doc.setMargins(40f, 40f, 40f, 40f)
                    addCoverPage(doc, notes.size)
                    notes.forEachIndexed { index, note ->
                        if (index > 0) doc.add(AreaBreak())
                        addNotePage(doc, note)
                    }
                }
            }
        }

        Timber.i("PdfExporter: Exported ${notes.size} notes to ${file.name}")
        file
    }.onFailure { e ->
        Timber.e(e, "PdfExporter: Export failed")
    }.getOrNull()

    private fun addCoverPage(doc: Document, noteCount: Int) {
        val darkBg = DeviceRgb(13, 17, 23)         // #0D1117
        val accent  = DeviceRgb(74, 144, 217)       // VaultPrimary

        doc.add(
            Paragraph("SecureVault")
                .setFontSize(36f)
                .setBold()
                .setFontColor(accent)
        )
        doc.add(
            Paragraph("Encrypted Notes Export")
                .setFontSize(18f)
                .setFontColor(ColorConstants.GRAY)
        )
        doc.add(Paragraph("\n"))
        doc.add(
            Paragraph("$noteCount note(s) exported")
                .setFontSize(12f)
        )
        doc.add(
            Paragraph("Generated: ${java.util.Date()}")
                .setFontSize(10f)
                .setFontColor(ColorConstants.GRAY)
        )
        doc.add(
            Paragraph("\nThis document is password-protected with AES-256 encryption.")
                .setFontSize(9f)
                .setFontColor(ColorConstants.GRAY)
        )
    }

    private fun addNotePage(doc: Document, note: Note) {
        val accent = DeviceRgb(74, 144, 217)

        // Title
        doc.add(
            Paragraph(note.title)
                .setFontSize(22f)
                .setBold()
                .setFontColor(accent)
        )

        // Metadata row
        val metaTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            .useAllAvailableWidth()
            .setMarginBottom(12f)

        metaTable.addCell(
            Cell().add(
                Paragraph("Updated: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(note.updatedAt)}")
                    .setFontSize(9f).setFontColor(ColorConstants.GRAY)
            ).setBorderBottom(com.itextpdf.kernel.pdf.canvas.draw.DottedLine())
             .setBorderTop(com.itextpdf.kernel.pdf.canvas.draw.DottedLine())
             .setPadding(4f)
        )

        val badges = buildString {
            if (note.isPinned)    append("📌 Pinned  ")
            if (note.isLocked)    append("🔒 Locked  ")
            if (note.isSensitive) append("⚠ Sensitive")
        }
        metaTable.addCell(
            Cell().add(
                Paragraph(badges).setFontSize(9f).setFontColor(ColorConstants.GRAY)
            ).setBorderBottom(com.itextpdf.kernel.pdf.canvas.draw.DottedLine())
             .setBorderTop(com.itextpdf.kernel.pdf.canvas.draw.DottedLine())
             .setPadding(4f)
        )

        doc.add(metaTable)

        // Content
        doc.add(
            Paragraph(note.content.ifBlank { "(empty)" })
                .setFontSize(12f)
                .setMultipliedLeading(1.5f)
        )
    }

    /** Utility: delete all previously exported files */
    fun clearExports() {
        File(context.filesDir, "exports")
            .listFiles()
            ?.forEach { it.delete() }
    }
}
