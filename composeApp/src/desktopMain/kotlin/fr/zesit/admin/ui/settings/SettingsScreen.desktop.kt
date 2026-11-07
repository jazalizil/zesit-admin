package fr.zesit.admin.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.NotificationsViewModel

/**
 * Variante desktop de la section Réglages : délègue pour l'instant à
 * l'agencement commun ([DefaultSettingsScreen]). Remplacer ce corps par un
 * agencement desktop dédié si besoin.
 */
@Composable
actual fun SettingsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier,
    onBack: () -> Unit,
) = DefaultSettingsScreen(viewModel, modifier, onBack)
