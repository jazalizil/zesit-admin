package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Variante desktop de la carte titre + légende + graphique : délègue pour
 * l'instant à [DefaultChartCard]. Remplacer ce corps pour un agencement desktop dédié.
 */
@Composable
internal actual fun ChartCard(
    title: String,
    legend: List<Pair<String, Color>>,
    modifier: Modifier,
    chart: @Composable () -> Unit,
) = DefaultChartCard(title, legend, modifier, chart)
