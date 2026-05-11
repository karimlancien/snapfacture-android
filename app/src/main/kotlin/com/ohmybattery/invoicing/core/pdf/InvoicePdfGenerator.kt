package com.ohmybattery.invoicing.core.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.ohmybattery.invoicing.core.money.Money
import com.ohmybattery.invoicing.data.local.entity.CompanyEntity
import com.ohmybattery.invoicing.data.local.entity.PaymentMethod
import com.ohmybattery.invoicing.data.local.relation.InvoiceWithDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoicePdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val dateFr = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    fun generate(invoice: InvoiceWithDetails, company: CompanyEntity): File {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        drawHeader(canvas, company, invoice)
        var cursor = MARGIN + 230f
        cursor = drawClientBlock(canvas, invoice, cursor)
        cursor = drawInvoiceMetaBlock(canvas, invoice, cursor)
        cursor = drawLinesTable(canvas, invoice, cursor + 24f)
        cursor = drawTotalsCard(canvas, invoice, cursor + 16f)
        drawPaidStamp(canvas, invoice, cursor + 18f)
        drawFooter(canvas, company)

        pdf.finishPage(page)

        val outDir = File(context.filesDir, "invoices").apply { mkdirs() }
        val file = File(outDir, "F-${invoice.invoice.number}.pdf")
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    fun shareUriFor(file: File): android.net.Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    // --- Drawing helpers ---------------------------------------------------

    private fun drawHeader(canvas: android.graphics.Canvas, company: CompanyEntity, inv: InvoiceWithDetails) {
        val bandPaint = Paint().apply { color = BRAND }
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 150f, bandPaint)

        val accentPaint = Paint().apply { color = ACCENT }
        canvas.drawRect(0f, 150f, PAGE_W.toFloat(), 156f, accentPaint)

        val title = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(company.name.uppercase(Locale.FRANCE), MARGIN, 70f, title)

        val sub = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            isAntiAlias = true
        }
        canvas.drawText("${company.addressLine}, ${company.postalCode} ${company.city}", MARGIN, 92f, sub)
        canvas.drawText("Tél. ${company.phone}  •  ${company.email}  •  ${company.website}", MARGIN, 110f, sub)
        canvas.drawText("SIREN ${company.siren}", MARGIN, 128f, sub)

        val factureLabel = Paint().apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("FACTURE N° ${inv.invoice.number}", PAGE_W - MARGIN, 70f, factureLabel)

        val factureDate = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("Émise le ${dateFr.format(Date(inv.invoice.issueDate))}", PAGE_W - MARGIN, 92f, factureDate)
    }

    private fun drawClientBlock(canvas: android.graphics.Canvas, inv: InvoiceWithDetails, top: Float): Float {
        val label = Paint().apply {
            color = MUTED
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("FACTURÉ À", MARGIN, top, label)

        val name = Paint().apply {
            color = INK
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(inv.client.name, MARGIN, top + 22f, name)

        val sub = Paint().apply { color = INK; textSize = 11f; isAntiAlias = true }
        var y = top + 40f
        listOfNotNull(
            inv.client.addressLine,
            listOfNotNull(inv.client.postalCode, inv.client.city)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { null },
            inv.client.phone,
            inv.client.email,
        ).forEach { line ->
            canvas.drawText(line, MARGIN, y, sub)
            y += 14f
        }

        val vehicleLine = listOfNotNull(inv.invoice.vehicleModel, inv.invoice.vehicleRegistration)
            .filter { it.isNotBlank() }
            .joinToString("  •  ")
        if (vehicleLine.isNotBlank()) {
            val vehicleLabel = Paint().apply {
                color = MUTED
                textSize = 9f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            y += 4f
            canvas.drawText("VÉHICULE", MARGIN, y, vehicleLabel)
            y += 13f
            canvas.drawText(vehicleLine, MARGIN, y, sub)
            y += 14f
        }
        return y
    }

    private fun drawInvoiceMetaBlock(canvas: android.graphics.Canvas, inv: InvoiceWithDetails, top: Float): Float {
        val xLabel = PAGE_W - MARGIN - 200f
        val xValue = PAGE_W - MARGIN

        val label = Paint().apply { color = MUTED; textSize = 10f; isAntiAlias = true }
        val value = Paint().apply {
            color = INK; textSize = 11f; isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val rows = buildList {
            add("Date d'émission" to dateFr.format(Date(inv.invoice.issueDate)))
            add("Échéance" to dateFr.format(Date(inv.invoice.dueDate)))
            inv.invoice.deliveryDate?.let { add("Livraison" to dateFr.format(Date(it))) }
            add("Mode de paiement" to paymentLabel(inv.invoice.paymentMethod))
        }

        var y = top - 20f
        rows.forEach { (l, v) ->
            canvas.drawText(l, xLabel, y, label)
            canvas.drawText(v, xValue, y, value)
            y += 18f
        }
        return maxOf(top, y)
    }

    private fun drawLinesTable(canvas: android.graphics.Canvas, inv: InvoiceWithDetails, top: Float): Float {
        val headerPaint = Paint().apply { color = SOFT_BG }
        canvas.drawRect(MARGIN, top, PAGE_W - MARGIN, top + 28f, headerPaint)

        val headLabel = Paint().apply {
            color = INK
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("DESCRIPTION", MARGIN + 8f, top + 18f, headLabel)
        canvas.drawText("QTÉ", MARGIN + 320f, top + 18f, headLabel.right())
        canvas.drawText("P.U. HT", MARGIN + 400f, top + 18f, headLabel.right())
        canvas.drawText("TVA", MARGIN + 460f, top + 18f, headLabel.right())
        canvas.drawText("TOTAL TTC", PAGE_W - MARGIN - 8f, top + 18f, headLabel.right())

        var y = top + 28f
        val rowDesc = Paint().apply { color = INK; textSize = 12f; isAntiAlias = true }
        val rowNote = Paint().apply { color = MUTED; textSize = 10f; isAntiAlias = true }
        val rowNum = Paint().apply {
            color = INK; textSize = 12f; isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val divider = Paint().apply { color = DIVIDER; strokeWidth = 0.6f }

        inv.lines.sortedBy { it.position }.forEach { l ->
            val rowH = if (l.extraNote.isNullOrBlank()) 32f else 48f
            canvas.drawText(l.description, MARGIN + 8f, y + 18f, rowDesc)
            l.extraNote?.takeIf { it.isNotBlank() }?.let {
                canvas.drawText(it, MARGIN + 8f, y + 33f, rowNote)
            }
            canvas.drawText(l.quantity.toString(), MARGIN + 320f, y + 18f, rowNum)
            canvas.drawText(Money.formatEurPlain(l.unitPriceHtCents), MARGIN + 400f, y + 18f, rowNum)
            canvas.drawText(Money.formatEurPlain(l.lineVatCents), MARGIN + 460f, y + 18f, rowNum)
            canvas.drawText(Money.formatEurPlain(l.lineTtcCents), PAGE_W - MARGIN - 8f, y + 18f, rowNum)
            y += rowH
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, divider)
        }
        return y
    }

    private fun drawTotalsCard(canvas: android.graphics.Canvas, inv: InvoiceWithDetails, top: Float): Float {
        val cardLeft = PAGE_W - MARGIN - 240f
        val cardRight = PAGE_W - MARGIN
        val cardBottom = top + 92f

        val card = Paint().apply { color = SOFT_BG }
        canvas.drawRoundRect(RectF(cardLeft, top, cardRight, cardBottom), 10f, 10f, card)

        val label = Paint().apply { color = MUTED; textSize = 11f; isAntiAlias = true }
        val value = Paint().apply {
            color = INK; textSize = 12f; isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val totalLabel = Paint().apply {
            color = BRAND; textSize = 14f; isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val totalValue = Paint().apply {
            color = BRAND; textSize = 18f; isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        var y = top + 22f
        canvas.drawText("Sous-total HT", cardLeft + 14f, y, label)
        canvas.drawText(Money.formatEurPlain(inv.invoice.totalHtCents), cardRight - 14f, y, value)
        y += 22f
        canvas.drawText("TVA (20%)", cardLeft + 14f, y, label)
        canvas.drawText(Money.formatEurPlain(inv.invoice.totalVatCents), cardRight - 14f, y, value)
        y += 28f
        canvas.drawText("TOTAL TTC", cardLeft + 14f, y, totalLabel)
        canvas.drawText(Money.formatEurPlain(inv.invoice.totalTtcCents), cardRight - 14f, y, totalValue)

        return cardBottom
    }

    private fun drawPaidStamp(canvas: android.graphics.Canvas, inv: InvoiceWithDetails, top: Float) {
        if (inv.invoice.paymentDate == null) return
        val tag = Paint().apply { color = ACCENT_SOFT }
        val rect = RectF(MARGIN, top, MARGIN + 200f, top + 36f)
        canvas.drawRoundRect(rect, 8f, 8f, tag)
        val label = Paint().apply {
            color = INK; textSize = 13f; isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("PAYÉE • ${paymentLabel(inv.invoice.paymentMethod)}", MARGIN + 14f, top + 23f, label)
    }

    private fun drawFooter(canvas: android.graphics.Canvas, company: CompanyEntity) {
        val divider = Paint().apply { color = DIVIDER; strokeWidth = 0.8f }
        canvas.drawLine(MARGIN, PAGE_H - 90f, PAGE_W - MARGIN, PAGE_H - 90f, divider)

        val small = Paint().apply { color = MUTED; textSize = 9f; isAntiAlias = true }
        canvas.drawText("${company.name} — SIREN ${company.siren}", MARGIN, PAGE_H - 70f, small)
        canvas.drawText("${company.addressLine}, ${company.postalCode} ${company.city}, ${company.country}", MARGIN, PAGE_H - 58f, small)
        canvas.drawText("Tél. ${company.phone}  •  ${company.email}  •  ${company.website}", MARGIN, PAGE_H - 46f, small)
        canvas.drawText("TVA non applicable, art. 293 B du CGI — sauf option contraire", MARGIN, PAGE_H - 32f, small)

        val signature = Paint().apply {
            color = MUTED; textSize = 10f; isAntiAlias = true; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("${company.managerName} • Gérant", PAGE_W - MARGIN, PAGE_H - 46f, signature)
    }

    private fun paymentLabel(method: PaymentMethod): String = when (method) {
        PaymentMethod.CASH -> "Espèces"
        PaymentMethod.TRANSFER -> "Virement"
        PaymentMethod.CARD -> "Carte bancaire"
        PaymentMethod.CHECK -> "Chèque"
        PaymentMethod.OTHER -> "Autre"
    }

    private fun Paint.right(): Paint = Paint(this).apply { textAlign = Paint.Align.RIGHT }

    companion object {
        // A4 @ 72dpi-ish: 595 x 842 points
        private const val PAGE_W = 595
        private const val PAGE_H = 842
        private const val MARGIN = 40f

        private val BRAND = Color.rgb(0x0D, 0x47, 0xA1)
        private val ACCENT = Color.rgb(0xFF, 0xB3, 0x00)
        private val ACCENT_SOFT = Color.rgb(0xFF, 0xE0, 0x82)
        private val INK = Color.rgb(0x11, 0x14, 0x18)
        private val MUTED = Color.rgb(0x6B, 0x74, 0x80)
        private val SOFT_BG = Color.rgb(0xF1, 0xF5, 0xFA)
        private val DIVIDER = Color.rgb(0xDF, 0xE5, 0xEC)
    }
}
