package com.ohmybattery.invoicing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohmybattery.invoicing.core.country.CountryProfile
import com.ohmybattery.invoicing.core.country.FranceProfile
import com.ohmybattery.invoicing.data.preferences.CountryPreferences
import com.ohmybattery.invoicing.data.repository.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StartupState(
    val needsOnboarding: Boolean,
    val profile: CountryProfile,
)

@HiltViewModel
class StartupViewModel @Inject constructor(
    companyRepo: CompanyRepository,
    countryPrefs: CountryPreferences,
) : ViewModel() {

    val state: StateFlow<StartupState?> = combine(
        companyRepo.observe(),
        countryPrefs.flow,
    ) { company, settings ->
        StartupState(
            needsOnboarding = company == null || company.name.isBlank(),
            profile = settings.profile,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
