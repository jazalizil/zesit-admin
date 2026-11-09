package fr.zesit.admin.ui.traffic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.TrafficViewModel

/** Actual Android de la section Traffic : agencement commun ([DefaultTrafficScreen]). */
@Composable
actual fun TrafficScreen(
    viewModel: TrafficViewModel,
    modifier: Modifier,
) = DefaultTrafficScreen(viewModel, modifier)
