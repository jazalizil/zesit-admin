package fr.zesit.admin.ui.realforecast

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.MedusaViewModel

/** Actual Android de la section Réel / Prévisionnel : agencement commun ([DefaultRealForecastScreen]). */
@Composable
actual fun RealForecastScreen(
    viewModel: MedusaViewModel,
    modifier: Modifier,
    onOpenSettings: () -> Unit,
) = DefaultRealForecastScreen(viewModel, modifier, onOpenSettings)
