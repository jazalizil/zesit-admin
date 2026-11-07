package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable

/**
 * Variante desktop du slider avec libellé : délègue pour l'instant à
 * [DefaultSliderRow]. Remplacer ce corps pour un agencement desktop dédié.
 */
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
