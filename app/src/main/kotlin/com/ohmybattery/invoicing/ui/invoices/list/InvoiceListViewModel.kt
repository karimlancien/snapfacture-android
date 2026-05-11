package com.ohmybattery.invoicing.ui.invoices.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohmybattery.invoicing.data.local.relation.InvoiceWithDetails
import com.ohmybattery.invoicing.data.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class InvoiceListUiState(
    val invoices: List<InvoiceWithDetails> = emptyList(),
    val monthRevenueCents: Long = 0L,
    val monthCount: Int = 0,
)

@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    repo: InvoiceRepository,
) : ViewModel() {

    private val monthStart: Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val state: StateFlow<InvoiceListUiState> =
        kotlinx.coroutines.flow.combine(
            repo.observeAll(),
            repo.observeRevenueSince(monthStart),
            repo.observeCountSince(monthStart),
        ) { invoices, revenue, count ->
            InvoiceListUiState(invoices, revenue ?: 0L, count)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceListUiState())
}
