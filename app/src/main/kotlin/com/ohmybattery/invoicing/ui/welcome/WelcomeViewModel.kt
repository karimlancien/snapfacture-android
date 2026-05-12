package com.ohmybattery.invoicing.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohmybattery.invoicing.data.preferences.CountryPreferences
import com.ohmybattery.invoicing.data.repository.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WelcomeUiState(
    val name: String = "",
    val countryCode: String = "FR",
    val manager: String = "",
    val taxOptedOut: Boolean = true,
    val saving: Boolean = false,
) {
    val canSave: Boolean get() = !saving && name.isNotBlank() && manager.isNotBlank()
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val companyRepo: CompanyRepository,
    private val countryPrefs: CountryPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(WelcomeUiState())
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onManagerChange(v: String) = _state.update { it.copy(manager = v) }
    fun onCountryChange(code: String) = _state.update {
        it.copy(countryCode = code, taxOptedOut = if (code == "US") false else it.taxOptedOut)
    }
    fun onTaxOptedOutChange(v: Boolean) = _state.update { it.copy(taxOptedOut = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val current = companyRepo.get() ?: return@launch
            companyRepo.update(
                current.copy(
                    name = s.name.trim(),
                    managerName = s.manager.trim(),
                    country = if (s.countryCode == "US") "United States" else "France",
                )
            )
            countryPrefs.setTaxOptedOut(if (s.countryCode == "US") false else s.taxOptedOut)
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }
}
