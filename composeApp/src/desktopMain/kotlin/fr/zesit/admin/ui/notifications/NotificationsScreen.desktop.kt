package fr.zesit.admin.ui.notifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.NotificationsViewModel

/**
 * Variante desktop de la section Notifications : délègue pour l'instant à
 * l'agencement commun ([DefaultNotificationsScreen]). Remplacer ce corps par un
 * agencement desktop dédié si besoin.
 */
@Composable
actual fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier,
) = DefaultNotificationsScreen(viewModel, modifier)
