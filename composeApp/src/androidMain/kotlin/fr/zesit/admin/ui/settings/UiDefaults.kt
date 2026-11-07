package fr.zesit.admin.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.NotificationsViewModel

/** Actual Android de la section Réglages : agencement commun ([DefaultSettingsScreen]). */
@Composable
actual fun SettingsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier,
    onBack: () -> Unit,
) = DefaultSettingsScreen(viewModel, modifier, onBack)
