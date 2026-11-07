package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import fr.zesit.admin.kpi.KpiFmt
import fr.zesit.admin.kpi.KpiFormatComputed
import fr.zesit.admin.kpi.KpiInputs
import fr.zesit.admin.kpi.KpiResponse
import fr.zesit.admin.viewmodel.KpiFormatKey
import fr.zesit.admin.viewmodel.KpiInputKey
import fr.zesit.admin.viewmodel.KpiUiState
import fr.zesit.admin.viewmodel.KpiViewModel

/**
 * Actuals iOS des composants KPI : délèguent aux implémentations communes
 * de référence (Default* de commonMain/ui/kpi). Pour un agencement iOS dédié,
 * remplacer le corps de l'actual concerné.
 */
@Composable
actual fun KpiScreen(viewModel: KpiViewModel, modifier: Modifier, onOpenSettings: () -> Unit) =
    DefaultKpiScreen(viewModel, modifier, onOpenSettings)

@Composable
actual fun KpiCard(label: String, value: String, sub: String?, tone: KpiTone, modifier: Modifier) =
    DefaultKpiCard(label, value, sub, tone, modifier)

@Composable
actual fun SliderRow(
    label: String,
    value: Double,
    min: Double,
    max: Double,
    step: Double,
    display: String,
    onValueChange: (Double) -> Unit,
) = DefaultSliderRow(label, value, min, max, step, display, onValueChange)

@Composable
actual fun NumRow(label: String, value: Double, onValueChange: (Double) -> Unit) =
    DefaultNumRow(label, value, onValueChange)

@Composable
internal actual fun FormatCardEditor(
    input: KpiFmt,
    computed: KpiFormatComputed?,
    onSet: (KpiFormatKey, Double) -> Unit,
) = DefaultFormatCardEditor(input, computed, onSet)

@Composable
internal actual fun HypothesesPanel(
    inputs: KpiInputs,
    model: KpiResponse?,
    onSet: (KpiInputKey, Double) -> Unit,
    modifier: Modifier,
) = DefaultHypothesesPanel(inputs, model, onSet, modifier)

@Composable
internal actual fun ChartLegend(items: List<Pair<String, Color>>, modifier: Modifier) =
    DefaultChartLegend(items, modifier)

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

@Composable
internal actual fun ChartCard(
    title: String,
    legend: List<Pair<String, Color>>,
    modifier: Modifier,
    chart: @Composable () -> Unit,
) = DefaultChartCard(title, legend, modifier, chart)

@Composable
actual fun MonthlyTable(model: KpiResponse, modifier: Modifier) = DefaultMonthlyTable(model, modifier)

@Composable
actual fun ModelsBar(
    state: KpiUiState,
    onSelect: (String?) -> Unit,
    onLoad: () -> Unit,
    onSave: (name: String, description: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier,
) = DefaultModelsBar(state, onSelect, onLoad, onSave, onDelete, modifier)
