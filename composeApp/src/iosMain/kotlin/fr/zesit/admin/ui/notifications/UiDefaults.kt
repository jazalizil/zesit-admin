package fr.zesit.admin.ui.notifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.NotificationsViewModel

/** Actual iOS de la section Notifications : agencement commun ([DefaultNotificationsScreen]). */
@Composable
actual fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier,
) = DefaultNotificationsScreen(viewModel, modifier)
