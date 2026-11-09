package fr.zesit.admin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.zesit.admin.bridge.ApnsBridge
import fr.zesit.admin.bridge.ApnsTokenHandler
import fr.zesit.admin.data.ThemeMode
import fr.zesit.admin.di.AppContainer
import fr.zesit.admin.ui.kpi.KpiScreen
import fr.zesit.admin.ui.nav.DashboardSection
import fr.zesit.admin.ui.notifications.NotificationsScreen
import fr.zesit.admin.ui.realforecast.RealForecastScreen
import fr.zesit.admin.ui.settings.SettingsScreen
import fr.zesit.admin.ui.theme.ZesitAdminTheme
import fr.zesit.admin.viewmodel.KpiViewModel
import fr.zesit.admin.viewmodel.MedusaViewModel
import fr.zesit.admin.viewmodel.NotificationsViewModel

/**
 * Racine de l'app (thème + ViewModels + navigation + sections du dashboard).
 * Déclarée « expect » : chaque plateforme fournit un `actual` dans son source
 * set (desktop : desktopMain — un fichier par composant), libre d'y définir
 * son propre agencement ; l'implémentation commune de référence est [DefaultApp].
 */
@Composable
expect fun App(container: AppContainer)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DefaultApp(container: AppContainer) {
    // Apparence choisie dans les Réglages : Système par défaut, Clair/Sombre forcés.
    val themeMode by container.themeRepository.themeMode.collectAsState()
    ZesitAdminTheme(
        darkTheme = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        },
    ) {
        val viewModel: NotificationsViewModel = viewModel { NotificationsViewModel(container) }
        val kpiViewModel: KpiViewModel = viewModel { KpiViewModel(container) }
        val medusaViewModel: MedusaViewModel =
            viewModel { MedusaViewModel(container, kpiViewModel.currentInputs) }

        // Branche le pont APNs : l'AppDelegate Swift pousse le device token ici (iOS uniquement)
        DisposableEffect(Unit) {
            ApnsBridge.setTokenHandler(ApnsTokenHandler { tokenHex -> viewModel.onApnsDeviceToken(tokenHex) })
            onDispose { ApnsBridge.clearTokenHandler() }
        }

        var showSettings by remember { mutableStateOf(false) }
        // Navigation du dashboard : KPI (accueil), Réel/Prévisionnel, Notifications
        var section by remember { mutableStateOf(DashboardSection.Kpi) }

        BoxWithConstraints {
            // Navigation responsive : rail latéral sur fenêtre large (desktop),
            // barre du bas sur écran étroit (iOS/Android).
            val useRail = maxWidth >= 600.dp

            Scaffold(
                // Plein écran : le fond couvre tout l'écran (status bar / home indicator
                // inclus) ; le contenu reste paddé par les safe-area insets (défaut).
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = { Text(if (showSettings) "Réglages" else section.label) },
                        actions = {
                            IconButton(onClick = { showSettings = !showSettings }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Réglages",
                                )
                            }
                        },
                    )
                },
                bottomBar = {
                    if (!useRail && !showSettings) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                            DashboardSection.entries.forEach { dest ->
                                NavigationBarItem(
                                    selected = section == dest,
                                    onClick = { section = dest },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    if (useRail && !showSettings) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.background,
                            header = {
                                Text(
                                    "Zesit Admin",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                        ) {
                            DashboardSection.entries.forEach { dest ->
                                NavigationRailItem(
                                    selected = section == dest,
                                    onClick = { section = dest },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) },
                                )
                            }
                        }
                    }

                    when {
                        showSettings -> SettingsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f),
                            onBack = { showSettings = false },
                        )
                        section == DashboardSection.Kpi -> KpiScreen(
                            viewModel = kpiViewModel,
                            modifier = Modifier.weight(1f),
                        )
                        section == DashboardSection.RealForecast -> RealForecastScreen(
                            viewModel = medusaViewModel,
                            modifier = Modifier.weight(1f),
                        )
                        section == DashboardSection.Notifications -> NotificationsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
