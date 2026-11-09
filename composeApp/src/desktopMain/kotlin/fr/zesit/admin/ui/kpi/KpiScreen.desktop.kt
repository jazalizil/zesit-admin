package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.KpiViewModel

/**
 * Variante desktop de la section KPI : délègue pour l'instant à l'agencement
 * commun ([DefaultKpiScreen]). Remplacer ce corps par un agencement desktop
 * dédié si besoin.
 */
@Composable
actual fun KpiScreen(
    viewModel: KpiViewModel,
    modifier: Modifier,
) = DefaultKpiScreen(viewModel, modifier)
