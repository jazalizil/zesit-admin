package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Variante desktop de la carte d'indicateur : délègue pour l'instant à
 * [DefaultKpiCard]. Remplacer ce corps pour un agencement desktop dédié.
 */
@Composable
actual fun KpiCard(
    label: String,
    value: String,
    sub: String?,
    tone: KpiTone,
    modifier: Modifier,
) = DefaultKpiCard(label, value, sub, tone, modifier)
