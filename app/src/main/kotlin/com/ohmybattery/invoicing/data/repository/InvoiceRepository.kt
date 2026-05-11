package com.ohmybattery.invoicing.data.repository

import androidx.room.withTransaction
import com.ohmybattery.invoicing.core.money.Money
import com.ohmybattery.invoicing.data.local.AppDatabase
import com.ohmybattery.invoicing.data.local.dao.AuditDao
import com.ohmybattery.invoicing.data.local.dao.CompanyDao
import com.ohmybattery.invoicing.data.local.dao.InvoiceDao
import com.ohmybattery.invoicing.data.local.entity.AuditLogEntity
import com.ohmybattery.invoicing.data.local.entity.InvoiceEntity
import com.ohmybattery.invoicing.data.local.entity.InvoiceLineEntity
import com.ohmybattery.invoicing.data.local.entity.InvoiceStatus
import com.ohmybattery.invoicing.data.local.entity.PaymentMethod
import com.ohmybattery.invoicing.data.local.relation.InvoiceWithDetails
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class DraftLine(
    val description: String,
    val extraNote: String?,
    val quantity: Int,
    val unitPriceTtcCents: Long,
    val vatRatePermille: Int = 200,
)

data class IssueInvoiceInput(
    val clientId: Long,
    val lines: List<DraftLine>,
    val paymentMethod: PaymentMethod,
    val issueDateMillis: Long,
    val deliveryDateMillis: Long?,
    val issuerName: String,
)

@Singleton
class InvoiceRepository @Inject constructor(
    private val db: AppDatabase,
    private val invoiceDao: InvoiceDao,
    private val companyDao: CompanyDao,
    private val auditDao: AuditDao,
) {

    fun observeAll(): Flow<List<InvoiceWithDetails>> = invoiceDao.observeAllWithDetails()

    fun observeRevenueSince(since: Long): Flow<Long?> = invoiceDao.observeRevenueSince(since)
    fun observeCountSince(since: Long): Flow<Int> = invoiceDao.observeCountSince(since)

    suspend fun get(id: Long): InvoiceWithDetails? = invoiceDao.getWithDetails(id)

    suspend fun issue(input: IssueInvoiceInput): Long = db.withTransaction {
        require(input.lines.isNotEmpty()) { "Aucune ligne sur la facture" }

        val computedLines = input.lines.map { l ->
            val ttcUnit = l.unitPriceTtcCents
            val htUnit = Money.htFromTtc(ttcUnit, l.vatRatePermille)
            val lineTtc = ttcUnit * l.quantity
            val lineHt = htUnit * l.quantity
            val lineVat = lineTtc - lineHt
            ComputedLine(
                description = l.description,
                extraNote = l.extraNote,
                quantity = l.quantity,
                unitHtCents = htUnit,
                vatRatePermille = l.vatRatePermille,
                lineHt = lineHt,
                lineVat = lineVat,
                lineTtc = lineTtc,
            )
        }

        val totalHt = computedLines.sumOf { it.lineHt }
        val totalVat = computedLines.sumOf { it.lineVat }
        val totalTtc = computedLines.sumOf { it.lineTtc }

        val number = companyDao.peekNextInvoiceNumber()
        companyDao.bumpInvoiceNumber()

        val invoice = InvoiceEntity(
            number = number,
            clientId = input.clientId,
            issueDate = input.issueDateMillis,
            dueDate = input.issueDateMillis,
            deliveryDate = input.deliveryDateMillis,
            totalHtCents = totalHt,
            totalVatCents = totalVat,
            totalTtcCents = totalTtc,
            paymentMethod = input.paymentMethod,
            paymentDate = input.issueDateMillis,
            status = InvoiceStatus.PAID,
            issuerName = input.issuerName,
        )
        val invoiceId = invoiceDao.insertInvoice(invoice)

        val lineRows = computedLines.mapIndexed { idx, c ->
            InvoiceLineEntity(
                invoiceId = invoiceId,
                description = c.description,
                extraNote = c.extraNote,
                quantity = c.quantity,
                unitPriceHtCents = c.unitHtCents,
                vatRatePermille = c.vatRatePermille,
                lineHtCents = c.lineHt,
                lineVatCents = c.lineVat,
                lineTtcCents = c.lineTtc,
                position = idx,
            )
        }
        invoiceDao.insertLines(lineRows)

        appendAudit(invoiceId, "INVOICE_ISSUED", number.toString() + "|" + totalTtc.toString())

        invoiceId
    }

    suspend fun attachPdf(invoiceId: Long, path: String) {
        invoiceDao.setPdfPath(invoiceId, path)
        appendAudit(invoiceId, "PDF_GENERATED", path)
    }

    private suspend fun appendAudit(invoiceId: Long?, event: String, payload: String) {
        val prev = auditDao.lastHash()
        val md = MessageDigest.getInstance("SHA-256")
        val raw = (prev ?: "") + "|" + event + "|" + payload + "|" + System.currentTimeMillis()
        val hash = md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
        auditDao.append(
            AuditLogEntity(
                invoiceId = invoiceId,
                event = event,
                payloadHash = hash,
                previousHash = prev,
            )
        )
    }

    private data class ComputedLine(
        val description: String,
        val extraNote: String?,
        val quantity: Int,
        val unitHtCents: Long,
        val vatRatePermille: Int,
        val lineHt: Long,
        val lineVat: Long,
        val lineTtc: Long,
    )
}
