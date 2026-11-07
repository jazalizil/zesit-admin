package fr.zesit.admin.ui.kpi

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.ui.common.SectionPlaceholder

/** Section KPI du dashboard — placeholders en attendant les données. */
@Composable
fun KpiScreen(modifier: Modifier = Modifier) {
    SectionPlaceholder(
        icon = Icons.Default.Insights,
        title = "KPI",
        message = "Les indicateurs clés s'afficheront ici. Données à brancher prochainement.",
        modifier = modifier,
    )
}
