package fr.zesit.admin.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sections du dashboard (navigation principale) — KPI est la section d'accueil.
 * [description] sert de sous-titre (menu latéral desktop / en-tête de section).
 *
 * [label] est le libellé compact (barre de navigation mobile) ; [longLabel]
 * est le libellé long des surfaces larges (menu latéral desktop / titre de
 * section) — version desktop et mobile d'une même section.
 */
enum class DashboardSection(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val longLabel: String = label,
) {
    Kpi(
        label = "KPI",
        description = "Prévisionnel année 1",
        icon = Icons.Default.Insights,
    ),
    RealForecast(
        label = "R/P",
        longLabel = "Réel/Prévisionnel",
        description = "Ventes réelles vs modèle",
        icon = Icons.Default.BarChart,
    ),
    Notifications(
        label = "Notifications",
        description = "Flux ntfy en temps réel",
        icon = Icons.Default.Notifications,
    ),
    Traffic(
        label = "Traffic",
        description = "Trafic web — traffic-inspector",
        icon = Icons.Default.Traffic,
    ),
    Security(
        label = "Sécurité",
        description = "Vulnérabilités des conteneurs",
        icon = Icons.Default.Security,
    ),
}
