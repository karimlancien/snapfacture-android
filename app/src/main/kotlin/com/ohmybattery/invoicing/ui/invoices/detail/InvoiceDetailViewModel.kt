package com.ohmybattery.invoicing.ui.invoices.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohmybattery.invoicing.core.pdf.InvoicePdfGenerator
import com.ohmybattery.invoicing.data.local.entity.CompanyEntity
import com.ohmybattery.invoicing.data.local.relation.InvoiceWithDetails
import com.ohmybattery.invoicing.data.repository.CompanyRepository
import com.ohmybattery.invoicing.data.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DetailUiState(
    val invoice: InvoiceWithDetails? = null,
    val company: CompanyEntity? = null,
    val pdfFile: File? = null,
)

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val invoiceRepo: InvoiceRepository,
    private val companyRepo: CompanyRepository,
    private val pdfGenerator: InvoicePdfGenerator,
) : ViewModel() {

    val invoiceId: Long = handle.get<Long>("invoiceId") ?: 0L

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val inv = invoiceRepo.get(invoiceId)
        val company = companyRepo.get()
        val file = inv?.invoice?.pdfPath?.let { File(it).takeIf(File::exists) }
        _state.update { it.copy(invoice = inv, company = company, pdfFile = file) }
    }

    fun regeneratePdf() = viewModelScope.launch {
        val inv = _state.value.invoice ?: return@launch
        val company = _state.value.company ?: return@launch
        val file = pdfGenerator.generate(inv, company)
        invoiceRepo.attachPdf(inv.invoice.id, file.absolutePath)
        _state.update { it.copy(pdfFile = file) }
    }
}
