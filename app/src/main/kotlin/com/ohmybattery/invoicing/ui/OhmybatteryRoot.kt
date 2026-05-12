package com.ohmybattery.invoicing.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ohmybattery.invoicing.ui.backup.BackupScreen
import com.ohmybattery.invoicing.ui.company.CompanyInfoScreen
import com.ohmybattery.invoicing.ui.csvexport.ExportScreen
import com.ohmybattery.invoicing.ui.csvimport.ImportScreen
import com.ohmybattery.invoicing.ui.catalog.CatalogScreen
import com.ohmybattery.invoicing.ui.invoices.create.CreateInvoiceScreen
import com.ohmybattery.invoicing.ui.invoices.detail.InvoiceDetailScreen
import com.ohmybattery.invoicing.ui.invoices.list.InvoiceListScreen
import com.ohmybattery.invoicing.ui.navigation.Routes
import com.ohmybattery.invoicing.ui.security.SecurityScreen
import com.ohmybattery.invoicing.ui.settings.SettingsScreen
import com.ohmybattery.invoicing.ui.stats.StatsScreen
import com.ohmybattery.invoicing.ui.welcome.WelcomeScreen

@Composable
fun OhmybatteryRoot(vm: StartupViewModel = hiltViewModel()) {
    val needsOnboarding by vm.needsOnboarding.collectAsStateWithLifecycle()
    val resolved = needsOnboarding
    if (resolved == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val nav = rememberNavController()
    val start = if (resolved) Routes.WELCOME else Routes.INVOICES
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onDone = {
                    nav.navigate(Routes.INVOICES) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.INVOICES) {
            InvoiceListScreen(
                onCreate = { nav.navigate(Routes.CREATE) },
                onOpen = { nav.navigate(Routes.detail(it)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onStats = { nav.navigate(Routes.STATS) },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.CREATE) {
            CreateInvoiceScreen(
                onBack = { nav.popBackStack() },
                onIssued = { id ->
                    nav.popBackStack()
                    nav.navigate(Routes.detail(id))
                },
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("invoiceId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("invoiceId") ?: 0L
            InvoiceDetailScreen(
                invoiceId = id,
                onBack = { nav.popBackStack() },
                onOpenInvoice = { other ->
                    nav.navigate(Routes.detail(other)) {
                        popUpTo(Routes.INVOICES)
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenCatalog = { nav.navigate(Routes.CATALOG) },
                onOpenImport = { nav.navigate(Routes.IMPORT) },
                onOpenExport = { nav.navigate(Routes.EXPORT) },
                onOpenBackup = { nav.navigate(Routes.BACKUP) },
                onOpenCompany = { nav.navigate(Routes.COMPANY) },
                onOpenSecurity = { nav.navigate(Routes.SECURITY) },
            )
        }
        composable(Routes.BACKUP) {
            BackupScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.COMPANY) {
            CompanyInfoScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SECURITY) {
            SecurityScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.CATALOG) {
            CatalogScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.IMPORT) {
            ImportScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.EXPORT) {
            ExportScreen(onBack = { nav.popBackStack() })
        }
    }
}
