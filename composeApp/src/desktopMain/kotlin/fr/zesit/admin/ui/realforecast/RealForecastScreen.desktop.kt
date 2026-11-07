package fr.zesit.admin.ui.realforecast

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.MedusaViewModel

/**
 * Variante desktop de la section Réel / Prévisionnel : délègue pour l'instant à
 * l'agencement commun ([DefaultRealForecastScreen]). Remplacer ce corps par un
 * agencement desktop dédié si besoin.
 */
@Composable
actual fun RealForecastScreen(
    viewModel: MedusaViewModel,
    modifier: Modifier,
    onOpenSettings: () -> Unit,
) = DefaultRealForecastScreen(viewModel, modifier, onOpenSettings)
