package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import fr.zesit.admin.kpi.KpiFmt
import fr.zesit.admin.kpi.KpiFormatComputed
import fr.zesit.admin.viewmodel.KpiFormatKey

/**
 * Variante desktop de la carte d'édition d'un format : délègue pour l'instant à
 * [DefaultFormatCardEditor]. Remplacer ce corps pour un agencement desktop dédié.
 */
@Composable
internal actual fun FormatCardEditor(
    input: KpiFmt,
    computed: KpiFormatComputed?,
    onSet: (KpiFormatKey, Double) -> Unit,
) = DefaultFormatCardEditor(input, computed, onSet)
