package com.ohmybattery.invoicing.ui.invoices.detail

import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohmybattery.invoicing.core.money.Money
import com.ohmybattery.invoicing.core.pdf.ShareInvoice
import com.ohmybattery.invoicing.data.local.entity.InvoiceType
import com.ohmybattery.invoicing.data.local.entity.PaymentMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    onBack: () -> Unit,
    onOpenInvoice: (Long) -> Unit,
    vm: InvoiceDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val inv = state.invoice

    var showCreditDialog by remember { mutableStateOf(false) }

    val isCredit = inv?.invoice?.type == InvoiceType.CREDIT_NOTE
    val titlePrefix = if (isCredit) "Avoir" else "Facture"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$titlePrefix ${inv?.invoice?.number?.let { "N° $it" } ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isCredit) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { pad ->
        if (inv == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isCredit && state.sourceInvoiceNumber != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Avoir sur facture N° ${state.sourceInvoiceNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                state.sourceInvoiceDate?.let {
                                    Text(
                                        "Émise le ${SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(it))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                            TextButton(
                                onClick = { inv.invoice.linkedInvoiceId?.let(onOpenInvoice) },
                            ) { Text("Voir") }
                        }
                    }
                }
            }
            if (!isCredit && state.linkedCreditNumber != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.Undo, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.size(12.dp))
                            Text(
                                "Annulée par l'avoir N° ${state.linkedCreditNumber}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        val df = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                        Text(inv.client.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("Émis${if (isCredit) "" else "e"} le ${df.format(Date(inv.invoice.issueDate))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Mode de paiement : ${labelFor(inv.invoice.paymentMethod)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inv.invoice.paymentNote?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("Motif : $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inv.invoice.comment?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("Commentaire : $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
}
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Lignes", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        inv.lines.sortedBy { it.position }.forEach { l ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text("${l.quantity} × ${l.description}")
                                    l.extraNote?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(Money.formatEurPlain(l.lineTtcCents))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        TotalRow("Total H.T.", inv.invoice.totalHtCents)
                        TotalRow("TVA (20%)", inv.invoice.totalVatCents)
                        Spacer(Modifier.height(4.dp))
                        TotalRow(
                            if (isCredit) "À rembourser" else "Total TTC",
                            inv.invoice.totalTtcCents,
                            big = true,
                            credit = isCredit,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val file = state.pdfFile ?: return@Button
                            context.startActivity(
                                ShareInvoice.intent(
                                    context = context,
                                    file = file,
                                    invoiceNumber = inv.invoice.number,
                                    recipientEmail = inv.client.email,
                                ),
                            )
                        },
                        enabled = state.pdfFile != null,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Partager")
                    }
                    OutlinedButton(
                        onClick = {
                            val file = state.pdfFile ?: return@OutlinedButton
                            val pm = context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
                            pm.print(
                                "${titlePrefix}-${inv.invoice.number}",
                                FilePrintAdapter(file),
                                PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .build(),
                            )
                        },
                        enabled = state.pdfFile != null,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Imprimer")
                    }
                }
            }
            if (!isCredit && state.linkedCreditNumber == null) {
                item {
                    OutlinedButton(
                        onClick = { showCreditDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Émettre un avoir (annuler cette facture)")
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = vm::regeneratePdf,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Régénérer le PDF") }
            }
        }
    }

    if (showCreditDialog) {
        CreditDialog(
            isSaving = state.isIssuingCredit,
            onDismiss = { showCreditDialog = false },
            onConfirm = { reason ->
                vm.issueCredit(reason) { newId ->
                    showCreditDialog = false
                    onOpenInvoice(newId)
                }
            },
        )
    }
}

@Composable
private fun CreditDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Émettre un avoir") },
        text = {
            Column {
                Text(
                    "Un avoir va être généré pour annuler cette facture. La facture originale reste conservée (obligation légale).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Motif (optionnel)") },
                    placeholder = { Text("Ex: erreur de saisie, retour batterie...") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.ifBlank { null }) },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Émettre l'avoir")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Annuler") }
        },
    )
}

@Composable
private fun TotalRow(label: String, cents: Long, big: Boolean = false, credit: Boolean = false) {
    val color = when {
        credit -> MaterialTheme.colorScheme.error
        big -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (big) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            Money.formatEurPlain(cents),
            style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (big) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun labelFor(m: PaymentMethod) = when (m) {
    PaymentMethod.CASH -> "Espèces"
    PaymentMethod.TRANSFER -> "Virement"
    PaymentMethod.CARD -> "Carte"
    PaymentMethod.CHECK -> "Chèque"
    PaymentMethod.OTHER -> "Autre"
}
