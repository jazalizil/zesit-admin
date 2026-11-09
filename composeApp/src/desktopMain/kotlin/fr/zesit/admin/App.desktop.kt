package fr.zesit.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.zesit.admin.data.ThemeMode
import fr.zesit.admin.di.AppContainer
import fr.zesit.admin.ui.kpi.KpiScreen
import fr.zesit.admin.ui.nav.DashboardSection
import fr.zesit.admin.ui.nav.SideMenu
import fr.zesit.admin.ui.notifications.NotificationsScreen
import fr.zesit.admin.ui.realforecast.RealForecastScreen
import fr.zesit.admin.ui.settings.SettingsScreen
import fr.zesit.admin.ui.theme.ZesitAdminTheme
import fr.zesit.admin.viewmodel.KpiViewModel
import fr.zesit.admin.viewmodel.MedusaViewModel
import fr.zesit.admin.viewmodel.NotificationsViewModel

/**
 * Variante desktop de la racine de l'app : layout à menu latéral —
 * inspiration « Ring Tracker — Settings Dashboard »
 * (dribbble.com/shots/27335762). Le fond de la fenêtre reprend la couleur de
 * marque du thème (primaire en clair, secondaire en sombre — voir
 * ui/theme/Theme.kt) ; le menu et le contenu de la section sélectionnée
 * flottent dessus. Les booléens d'état (section courante, réglages ouverts)
 * et les ViewModels sont les mêmes que dans [DefaultApp] (mobile).
 */
@Composable
actual fun App(container: AppContainer) {
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

        // Navigation du dashboard : KPI (accueil), Réel/Prévisionnel, Notifications
        var section by remember { mutableStateOf(DashboardSection.Kpi) }
        var showSettings by remember { mutableStateOf(false) }
        val selectSection: (DashboardSection) -> Unit = {
            section = it
            showSettings = false
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            SideMenu(
                selected = section,
                settingsOpen = showSettings,
                onSelectSection = selectSection,
                onToggleSettings = { showSettings = !showSettings },
                modifier = Modifier
                    .width(268.dp)
                    .padding(14.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 28.dp, start = 20.dp, end = 24.dp, bottom = 20.dp),
            ) {
                // En-tête de la section (pas de TopAppBar : le menu fait office de barre)
                Text(
                    if (showSettings) "Réglages" else section.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    if (showSettings) "Apparence, données, version" else section.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                when {
                    showSettings -> SettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onBack = { showSettings = false },
                    )
                    section == DashboardSection.Kpi -> KpiScreen(
                        viewModel = kpiViewModel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    section == DashboardSection.RealForecast -> RealForecastScreen(
                        viewModel = medusaViewModel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    section == DashboardSection.Notifications -> NotificationsScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}
