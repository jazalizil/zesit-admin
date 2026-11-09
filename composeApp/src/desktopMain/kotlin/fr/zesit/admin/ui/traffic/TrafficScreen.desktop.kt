package fr.zesit.admin.ui.traffic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.TrafficViewModel

/**
 * Variante desktop de la section Traffic : délègue à l'agencement commun
 * ([DefaultTrafficScreen]). Remplacer ce corps par un agencement desktop
 * dédié si besoin.
 */
@Composable
actual fun TrafficScreen(
    viewModel: TrafficViewModel,
    modifier: Modifier,
) = DefaultTrafficScreen(viewModel, modifier)
