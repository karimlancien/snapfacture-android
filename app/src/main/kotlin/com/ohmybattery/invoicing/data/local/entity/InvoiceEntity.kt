package com.ohmybattery.invoicing.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class InvoiceStatus { DRAFT, ISSUED, PAID, CANCELLED }
enum class PaymentMethod { CASH, TRANSFER, CARD, CHECK, OTHER }

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("clientId"), Index(value = ["number"], unique = true)]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: Int,
    val clientId: Long,
    val issueDate: Long,
    val dueDate: Long,
    val deliveryDate: Long? = null,
    val totalHtCents: Long,
    val totalVatCents: Long,
    val totalTtcCents: Long,
    val discountCents: Long = 0,
    val currency: String = "EUR",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentDate: Long? = null,
    val paymentReference: String? = null,
    val paymentNote: String? = null,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val issuerName: String,
    val pdfPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
