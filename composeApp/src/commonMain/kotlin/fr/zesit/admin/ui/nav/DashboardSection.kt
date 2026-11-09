package fr.zesit.admin.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sections du dashboard (navigation principale) — KPI est la section d'accueil.
 * [description] sert de sous-titre (menu latéral desktop / en-tête de section).
 */
enum class DashboardSection(val label: String, val description: String, val icon: ImageVector) {
    Kpi(
        label = "KPI",
        description = "Prévisionnel année 1",
        icon = Icons.Default.Insights,
    ),
    RealForecast(
        label = "Réel / Prévisionnel",
        description = "Ventes réelles vs modèle",
        icon = Icons.Default.BarChart,
    ),
    Notifications(
        label = "Notifications",
        description = "Flux ntfy en temps réel",
        icon = Icons.Default.Notifications,
    ),
}
