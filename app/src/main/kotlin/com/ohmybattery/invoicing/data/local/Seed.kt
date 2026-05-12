package com.ohmybattery.invoicing.data.local

import com.ohmybattery.invoicing.data.local.entity.CompanyEntity
import com.ohmybattery.invoicing.data.local.entity.ProductEntity

object Seed {

    const val START_INVOICE_NUMBER = 1

    val Company = CompanyEntity(
        id = 1,
        name = "",
        legalForm = "",
        siren = "",
        vatNumber = null,
        addressLine = "",
        postalCode = "",
        city = "",
        country = "France",
        phone = "",
        email = "",
        website = "",
        managerName = "",
        iban = null,
        nextInvoiceNumber = START_INVOICE_NUMBER,
    )

    val Catalog: List<ProductEntity> = emptyList()
}
