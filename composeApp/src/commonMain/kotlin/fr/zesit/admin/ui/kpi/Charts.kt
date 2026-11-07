package fr.zesit.admin.ui.kpi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// Palette des graphiques du dashboard web (recharts) — conservée à l'identique.
internal val ChartBarPositive = Color(0xFF10B981) // emerald-500
internal val ChartBarNegative = Color(0xFFEF4444) // red-500
internal val ChartLineTeal = Color(0xFF0F766E)   // teal-700
internal val ChartAreaAmber = Color(0xFFF59E0B)   // amber-500
internal val ChartLineSky = Color(0xFF0EA5E9)     // sky-500
internal val ChartZeroSlate = Color(0xFF94A3B8)   // slate-400
internal val ChartZeroRed = Color(0xFFEF4444)

/** Une série à tracer : barres (1 à 2) ou lignes (1 à 2, pointillées, aires). */
internal data class SeriesSpec(
    val values: List<Double>,
    val color: Color,
    /** Barres colorées par signe (vert/rouge) comme le compte de résultat du web. */
    val signColored: Boolean = false,
    /** Ligne pointillée (ex. cumul prévu du réel vs prévu). */
    val dashed: Boolean = false,
    /** Aire remplie sous la ligne (ex. trésorerie). */
    val area: Boolean = false,
)

/** Légende commune : pastilles colorées + libellés (miroir de <Legend /> de recharts). */
@Composable
internal fun ChartLegend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Graphique Canvas générique : 1–2 séries de barres (côte à côte si deux) et/ou
 * 1–2 lignes, axes Y gradués (nice steps), labels X, ligne zéro, référence
 * verticale optionnelle. Toutes les séries sont alignées sur [xLabels].
 */
@OptIn(ExperimentalTextApi::class)
@Composable
internal fun Chart(
    xLabels: List<String>,
    bars: List<SeriesSpec>,
    lines: List<SeriesSpec>,
    modifier: Modifier = Modifier,
    height: Dp = 320.dp,
    refIndex: Int? = null,
    refLabel: String? = null,
    zeroColor: Color = ChartZeroSlate,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val count = xLabels.size
        if (count == 0) return@Canvas
        val allSeries = (bars + lines).flatMap { it.values }
        if (allSeries.isEmpty()) return@Canvas

        val left = 52.dp.toPx()
        val top = 18.dp.toPx()
        val right = 10.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val plotW = size.width - left - right
        val plotH = size.height - top - bottomPad
        if (plotW <= 0 || plotH <= 0) return@Canvas

        // Domaine Y : barres -> inclut toujours 0 ; lignes/aires -> [min, max] + padding.
        val rawMin = allSeries.min()
        val rawMax = allSeries.max()
        var lo = if (bars.isNotEmpty()) min(0.0, rawMin) else rawMin
        var hi = if (bars.isNotEmpty()) max(0.0, rawMax) else rawMax
        if (hi - lo < 1e-9) hi = lo + max(1.0, abs(lo) * 0.1)
        val pad = (hi - lo) * 0.08
        lo -= pad
        hi += pad
        val step = niceStep(hi - lo)
        lo = floor(lo / step) * step
        hi = ceil(hi / step) * step
        val span = (hi - lo).coerceAtLeast(1.0)

        fun yFor(v: Double): Float = (top + ((hi - v) / span) * plotH).toFloat()
        fun xFor(i: Int): Float = left + (plotW / count) * (i + 0.5f)

        // Graduations + grille horizontale
        var tick = lo
        while (tick <= hi + 1e-9) {
            val y = yFor(tick)
            drawLine(gridColor, Offset(left, y), Offset(size.width - right, y), strokeWidth = 1f)
            val label = textMeasurer.measure(fmtAxisEuro(tick), labelStyle)
            drawText(
                label,
                color = axisTextColor,
                topLeft = Offset(left - label.size.width - 6.dp.toPx(), y - label.size.height / 2f),
            )
            tick += step
        }

        // Labels X (avec saut pour éviter le chevauchement)
        val slotW = plotW / count
        val xLabelLayouts = xLabels.map { textMeasurer.measure(it, labelStyle) }
        val maxLabelW = xLabelLayouts.maxOf { it.size.width }
        val stride = max(1, ceil(maxLabelW / slotW).toInt())
        xLabelLayouts.forEachIndexed { i, layout ->
            if (i % stride == 0) {
                drawText(
                    layout,
                    color = axisTextColor,
                    topLeft = Offset(
                        xFor(i) - layout.size.width / 2f,
                        size.height - bottomPad + 6.dp.toPx(),
                    ),
                )
            }
        }

        // Ligne zéro (au-dessus de la grille)
        val zeroY = yFor(0.0)
        if (zeroY in top..(top + plotH)) {
            drawLine(zeroColor, Offset(left, zeroY), Offset(size.width - right, zeroY), strokeWidth = 1.5f)
        }

        // Barres : une série -> barre pleine dans son slot ; deux -> barres jumelles
        if (bars.isNotEmpty()) {
            val barW = if (bars.size == 1) slotW * 0.62f else slotW * 0.3f
            val radius = CornerRadius(5f, 5f)
            bars.forEachIndexed { s, series ->
                val shift = if (bars.size == 1) 0f else if (s == 0) -slotW * 0.17f else slotW * 0.17f
                series.values.forEachIndexed { i, v ->
                    val y = yFor(v)
                    val topY = min(y, zeroY)
                    val h = max(2f, abs(zeroY - y))
                    drawRoundRect(
                        color = if (series.signColored && v < 0) ChartBarNegative else series.color,
                        topLeft = Offset(xFor(i) + shift - barW / 2f, topY),
                        size = Size(barW, h),
                        cornerRadius = radius,
                    )
                }
            }
        }

        // Lignes (aire remplie jusqu'au zéro si demandé)
        lines.forEach { series ->
            val path = Path()
            series.values.forEachIndexed { i, v ->
                val p = Offset(xFor(i), yFor(v))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            if (series.area) {
                val area = Path().apply {
                    addPath(path)
                    lineTo(xFor(series.values.size - 1), zeroY)
                    lineTo(xFor(0), zeroY)
                    close()
                }
                drawPath(area, series.color.copy(alpha = 0.3f))
            }
            drawPath(
                path,
                series.color,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = if (series.dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null,
                ),
            )
        }

        // Référence verticale (ex. hypothèse pâte courante sur la courbe de sensibilité)
        if (refIndex != null && refIndex in xLabels.indices) {
            val x = xFor(refIndex)
            drawLine(
                ChartLineTeal,
                Offset(x, top),
                Offset(x, top + plotH),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            )
            if (!refLabel.isNullOrEmpty()) {
                val layout = textMeasurer.measure(refLabel, labelStyle)
                drawText(
                    layout,
                    color = ChartLineTeal,
                    topLeft = Offset(
                        (x - layout.size.width / 2f).coerceIn(left, size.width - right - layout.size.width),
                        top - 14.dp.toPx(),
                    ),
                )
            }
        }
    }
}

/** Titre + légende + graphique dans une carte (structure des cards du web). */
@Composable
internal fun ChartCard(
    title: String,
    legend: List<Pair<String, Color>>,
    modifier: Modifier = Modifier,
    chart: @Composable () -> Unit,
) {
    androidx.compose.material3.Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(bottom = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            )
            if (legend.isNotEmpty()) {
                ChartLegend(legend, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            chart()
        }
    }
}
