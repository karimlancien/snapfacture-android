package com.ohmybattery.invoicing.core.money

import java.text.NumberFormat
import java.util.Locale

/** All monetary amounts are stored as Long cents to avoid floating-point drift. */
object Money {

    private val FR: Locale = Locale.forLanguageTag("fr-FR")

    fun htFromTtc(ttcCents: Long, vatRatePermille: Int): Long {
        val ht = (ttcCents.toDouble() * 1000.0) / (1000.0 + vatRatePermille.toDouble())
        return Math.round(ht)
    }

    fun vatFromTtc(ttcCents: Long, vatRatePermille: Int): Long =
        ttcCents - htFromTtc(ttcCents, vatRatePermille)

    fun formatEur(cents: Long): String {
        val f = NumberFormat.getCurrencyInstance(FR)
        return f.format(cents / 100.0)
    }

    fun formatEurPlain(cents: Long): String {
        val nf = NumberFormat.getNumberInstance(FR).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return nf.format(cents / 100.0) + " €"
    }

    fun toDouble(cents: Long): Double = cents / 100.0
}
