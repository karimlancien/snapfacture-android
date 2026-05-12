package com.ohmybattery.invoicing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohmybattery.invoicing.data.repository.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    companyRepo: CompanyRepository,
) : ViewModel() {

    val needsOnboarding: StateFlow<Boolean?> = companyRepo.observe()
        .map { company -> company == null || company.name.isBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
