package com.ohmybattery.invoicing.ui.csvimport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    vm: ImportViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.run(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importer un CSV") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Intro() }

            when (val phase = state.phase) {
                ImportPhase.Idle -> item {
                    Button(
                        onClick = {
                            picker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/csv", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.padding(end = 8.dp))
                        Text("Choisir un fichier .csv")
                    }
                }
                ImportPhase.Running -> item { RunningCard() }
                is ImportPhase.Done -> item { DoneCard(phase, onPickAgain = { vm.reset() }, onBack = onBack) }
                is ImportPhase.Error -> item { ErrorCard(phase, onRetry = { vm.reset() }) }
            }
        }
    }
}

@Composable
private fun Intro() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Reprendre votre historique",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Importez le CSV exporté de votre ancien outil pour récupérer toutes les factures précédentes. Les numéros déjà présents sont ignorés. Le compteur de numérotation est automatiquement mis à jour.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RunningCard() {
    Card {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Import en cours...")
        }
    }
}

@Composable
private fun DoneCard(phase: ImportPhase.Done, onPickAgain: () -> Unit, onBack: () -> Unit) {
    val r = phase.report
    Card {
        Column(Modifier.padding(20.dp)) {
            Text("Import terminé", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            StatRow("Factures importées", r.imported.toString())
            StatRow("Ignorées", r.skipped.toString())
            r.maxImportedNumber?.let { StatRow("N° max importé", it.toString()) }
            if (r.errors.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Détails", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                r.errors.take(20).forEach { err ->
                    Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (r.errors.size > 20) {
                    Text("… (+${r.errors.size - 20} autres)", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Retour") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onPickAgain, modifier = Modifier.fillMaxWidth()) {
                Text("Importer un autre fichier")
            }
        }
    }
}

@Composable
private fun ErrorCard(phase: ImportPhase.Error, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text("Import impossible", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(8.dp))
            Text(phase.message, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Réessayer") }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
