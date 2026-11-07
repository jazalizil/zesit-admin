package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Variante desktop de la légende de graphique : délègue pour l'instant à
 * [DefaultChartLegend]. Remplacer ce corps pour un agencement desktop dédié.
 */
@Composable
internal actual fun ChartLegend(
    items: List<Pair<String, Color>>,
    modifier: Modifier,
) = DefaultChartLegend(items, modifier)
