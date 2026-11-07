package fr.zesit.admin

import androidx.compose.runtime.Composable
import fr.zesit.admin.di.AppContainer

/**
 * Variante desktop de la racine de l'app : délègue pour l'instant à
 * l'agencement commun ([DefaultApp]). Remplacer ce corps par un agencement
 * desktop dédié si besoin — les sections (KpiScreen, RealForecastScreen,
 * NotificationsScreen, SettingsScreen) se composent comme dans commonMain.
 */
@Composable
actual fun App(container: AppContainer) = DefaultApp(container)
