package fr.zesit.admin.ui.realforecast

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.ui.common.SectionPlaceholder

/** Section « Réel / Prévisionnel » du dashboard — placeholder en attendant les données. */
@Composable
fun RealForecastScreen(modifier: Modifier = Modifier) {
    SectionPlaceholder(
        icon = Icons.Default.BarChart,
        title = "Réel / Prévisionnel",
        message = "La comparaison du réalisé et du prévisionnel s'affichera ici. Données à brancher prochainement.",
        modifier = modifier,
    )
}
