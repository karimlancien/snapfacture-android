package com.ohmybattery.invoicing.ui.invoices.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohmybattery.invoicing.core.money.Money
import com.ohmybattery.invoicing.core.pdf.InvoicePdfGenerator
import com.ohmybattery.invoicing.data.local.entity.BatteryEntity
import com.ohmybattery.invoicing.data.local.entity.ClientEntity
import com.ohmybattery.invoicing.data.local.entity.PaymentMethod
import com.ohmybattery.invoicing.data.repository.BatteryRepository
import com.ohmybattery.invoicing.data.repository.ClientRepository
import com.ohmybattery.invoicing.data.repository.CompanyRepository
import com.ohmybattery.invoicing.data.repository.DraftLine
import com.ohmybattery.invoicing.data.repository.InvoiceRepository
import com.ohmybattery.invoicing.data.repository.IssueInvoiceInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartLine(
    val battery: BatteryEntity,
    val quantity: Int,
)

data class CreateUiState(
    val clientName: String = "",
    val matchingClients: List<ClientEntity> = emptyList(),
    val selectedClient: ClientEntity? = null,
    val cart: List<CartLine> = emptyList(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val totalTtcCents: Long get() = cart.sumOf { it.battery.priceTtcCents * it.quantity }
    val totalHtCents: Long get() = cart.sumOf {
        val ht = Money.htFromTtc(it.battery.priceTtcCents, it.battery.vatRatePermille)
        ht * it.quantity
    }
    val totalVatCents: Long get() = totalTtcCents - totalHtCents
    val canIssue: Boolean get() =
        !isSaving && cart.isNotEmpty() && (clientName.isNotBlank() || selectedClient != null)
}

@HiltViewModel
class CreateInvoiceViewModel @Inject constructor(
    private val clientRepo: ClientRepository,
    batteryRepo: BatteryRepository,
    private val companyRepo: CompanyRepository,
    private val invoiceRepo: InvoiceRepository,
    private val pdfGenerator: InvoicePdfGenerator,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    val catalog: StateFlow<List<BatteryEntity>> =
        batteryRepo.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onClientNameChange(name: String) {
        _state.update { it.copy(clientName = name, selectedClient = null) }
        viewModelScope.launch {
            _state.update { it.copy(matchingClients = clientRepo.search(name)) }
        }
    }

    fun selectClient(c: ClientEntity) {
        _state.update { it.copy(selectedClient = c, clientName = c.name, matchingClients = emptyList()) }
    }

    fun addBattery(b: BatteryEntity) {
        _state.update { st ->
            val idx = st.cart.indexOfFirst { it.battery.id == b.id }
            val newCart = if (idx >= 0) {
                st.cart.toMutableList().also { it[idx] = it[idx].copy(quantity = it[idx].quantity + 1) }
            } else st.cart + CartLine(b, 1)
            st.copy(cart = newCart)
        }
    }

    fun decrement(b: BatteryEntity) {
        _state.update { st ->
            val idx = st.cart.indexOfFirst { it.battery.id == b.id }
            if (idx < 0) return@update st
            val current = st.cart[idx]
            val newCart = if (current.quantity <= 1) {
                st.cart.toMutableList().also { it.removeAt(idx) }
            } else {
                st.cart.toMutableList().also { it[idx] = current.copy(quantity = current.quantity - 1) }
            }
            st.copy(cart = newCart)
        }
    }

    fun setPaymentMethod(m: PaymentMethod) {
        _state.update { it.copy(paymentMethod = m) }
    }

    /** Issues the invoice + generates the PDF. Returns the persisted invoice id. */
    fun issue(onIssued: (Long) -> Unit) {
        val st = _state.value
        if (!st.canIssue) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val clientId = st.selectedClient?.id
                    ?: clientRepo.insert(ClientEntity(name = st.clientName.trim()))

                val now = System.currentTimeMillis()
                val lines = st.cart.map { line ->
                    DraftLine(
                        description = line.battery.label,
                        extraNote = if (line.battery.withInstall) {
                            "Changement de batterie effectué par notre technicien chez le client"
                        } else null,
                        quantity = line.quantity,
                        unitPriceTtcCents = line.battery.priceTtcCents,
                        vatRatePermille = line.battery.vatRatePermille,
                    )
                }
                val invoiceId = invoiceRepo.issue(
                    IssueInvoiceInput(
                        clientId = clientId,
                        lines = lines,
                        paymentMethod = st.paymentMethod,
                        issueDateMillis = now,
                        deliveryDateMillis = if (lines.any { it.extraNote != null }) now else null,
                        issuerName = "Jubs",
                    )
                )
                val company = companyRepo.get() ?: error("Company missing")
                val details = invoiceRepo.get(invoiceId) ?: error("Invoice missing")
                val file = pdfGenerator.generate(details, company)
                invoiceRepo.attachPdf(invoiceId, file.absolutePath)
                _state.update { it.copy(isSaving = false) }
                onIssued(invoiceId)
            } catch (t: Throwable) {
                _state.update { it.copy(isSaving = false, error = t.message ?: "Erreur") }
            }
        }
    }
}
