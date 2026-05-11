package com.ohmybattery.invoicing.data.local

import com.ohmybattery.invoicing.data.local.entity.BatteryEntity
import com.ohmybattery.invoicing.data.local.entity.CompanyEntity

object Seed {

    const val START_INVOICE_NUMBER = 1693

    val Company = CompanyEntity(
        id = 1,
        name = "Ohmybattery",
        legalForm = "",
        siren = "887714228",
        vatNumber = null,
        addressLine = "26 Avenue Léon Blum",
        postalCode = "93190",
        city = "Livry-Gargan",
        country = "France",
        phone = "07 67 47 37 62",
        email = "team@ohmybattery.fr",
        website = "ohmybattery.fr",
        managerName = "M. Abbes",
        iban = null,
        nextInvoiceNumber = START_INVOICE_NUMBER,
    )

    val Catalog: List<BatteryEntity> = listOf(
        BatteryEntity(label = "Torus 50Ah 420A", priceTtcCents = 8_000, withInstall = false, sortOrder = 1),
        BatteryEntity(label = "Torus 60Ah 540A", priceTtcCents = 9_000, withInstall = false, sortOrder = 2),
        BatteryEntity(label = "Torus 70Ah 640A", priceTtcCents = 10_000, withInstall = false, sortOrder = 3),
        BatteryEntity(label = "Torus 95Ah 750A", priceTtcCents = 13_000, withInstall = false, sortOrder = 4),
        BatteryEntity(label = "Torus 60Ah 540A + pose domicile", priceTtcCents = 16_000, withInstall = true, sortOrder = 5),
        BatteryEntity(label = "Torus 60Ah 540A + pose domicile (var.)", priceTtcCents = 17_000, withInstall = true, sortOrder = 6),
        BatteryEntity(label = "Torus 60Ah 540A + pose domicile (var.)", priceTtcCents = 18_000, withInstall = true, sortOrder = 7),
        BatteryEntity(label = "Torus 60Ah Start & Stop + pose", priceTtcCents = 20_000, withInstall = true, sortOrder = 8),
        BatteryEntity(label = "Torus 60Ah Start & Stop 680A + pose", priceTtcCents = 22_000, withInstall = true, sortOrder = 9),
    )
}
