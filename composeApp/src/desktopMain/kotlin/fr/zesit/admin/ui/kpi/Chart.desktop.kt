package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Variante desktop du graphique Canvas : délègue pour l'instant à
 * [DefaultChart]. Remplacer ce corps pour un rendu desktop dédié.
 */
@Composable
internal actual fun Chart(
    xLabels: List<String>,
    bars: List<SeriesSpec>,
    lines: List<SeriesSpec>,
    modifier: Modifier,
    height: Dp,
    refIndex: Int?,
    refLabel: String?,
    zeroColor: Color,
) = DefaultChart(xLabels, bars, lines, modifier, height, refIndex, refLabel, zeroColor)
