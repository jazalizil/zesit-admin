package fr.zesit.admin.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

/** Sections du dashboard (navigation principale) — KPI est la section d'accueil. */
enum class DashboardSection(val label: String, val icon: ImageVector) {
    Kpi(label = "KPI", icon = Icons.Default.Insights),
    RealForecast(label = "Réel / Prévisionnel", icon = Icons.Default.BarChart),
    Notifications(label = "Notifications", icon = Icons.Default.Notifications),
}
