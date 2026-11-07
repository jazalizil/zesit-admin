package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.kpi.KpiInputs
import fr.zesit.admin.kpi.KpiResponse
import fr.zesit.admin.viewmodel.KpiInputKey

/**
 * Variante desktop du panneau des hypothèses globales : délègue pour l'instant
 * à [DefaultHypothesesPanel]. Remplacer ce corps pour un agencement desktop dédié.
 */
@Composable
internal actual fun HypothesesPanel(
    inputs: KpiInputs,
    model: KpiResponse?,
    onSet: (KpiInputKey, Double) -> Unit,
    modifier: Modifier,
) = DefaultHypothesesPanel(inputs, model, onSet, modifier)
