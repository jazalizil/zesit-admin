package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable

/**
 * Variante desktop du champ numérique borné : délègue pour l'instant à
 * [DefaultNumRow]. Remplacer ce corps pour un agencement desktop dédié.
 */
@Composable
actual fun NumRow(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
) = DefaultNumRow(label, value, onValueChange)
