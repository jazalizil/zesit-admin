package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.kpi.KpiResponse

/**
 * Variante desktop du compte de résultat mensuel : délègue pour l'instant à
 * [DefaultMonthlyTable]. Remplacer ce corps pour un agencement desktop dédié.
 */
@Composable
actual fun MonthlyTable(
    model: KpiResponse,
    modifier: Modifier,
) = DefaultMonthlyTable(model, modifier)
