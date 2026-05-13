package com.ohmybattery.invoicing.core.country

import androidx.compose.runtime.compositionLocalOf

/**
 * Active CountryProfile available to any composable below the provider.
 * The provider lives in OhmybatteryRoot and tracks the company.country field.
 */
val LocalCountryProfile = compositionLocalOf<CountryProfile> { FranceProfile }
