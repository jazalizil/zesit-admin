package fr.zesit.admin.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.zesit.admin.model.Notification
import fr.zesit.admin.ui.common.NtfyAuthPanel
import fr.zesit.admin.viewmodel.NotificationsUiState
import fr.zesit.admin.viewmodel.NotificationsViewModel
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/**
 * Section Notifications — « expect » : l'agencement par plateforme vit dans
 * l'`actual` (desktop : desktopMain) ; l'agencement commun de référence est
 * [DefaultNotificationsScreen].
 */
@Composable
expect fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier,
)

@Composable
internal fun DefaultNotificationsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    when {
        // Serveur ntfy non configuré : panneau de connexion sur la page même
        // (remplace l'ancien renvoi vers les Réglages).
        !state.configured -> NtfyAuthPanel(
            savedTopic = settings.topic,
            baseUrl = viewModel.env.ntfyBaseUrlDefault,
            tokenAvailable = viewModel.hasSecretToken(),
            error = state.lastError,
            onConnect = viewModel::connectNtfy,
            modifier = modifier,
        )
        else -> ConnectedNotifications(state, viewModel, modifier)
    }
}

@Composable
private fun ConnectedNotifications(
    state: NotificationsUiState,
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Bandeau de statut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.refreshing) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (state.connected) "Connecté (temps réel)" else "Hors ligne — poll manuel",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.connected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.lastError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                state.apnsStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            IconButton(onClick = viewModel::disconnectNtfy) {
                Icon(Icons.Default.PowerOff, contentDescription = "Déconnecter")
            }
        }

        if (state.notifications.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Aucune notification pour le moment.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(state.notifications, key = { it.id }) { notif ->
                    NotificationCard(notif)
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notif: Notification) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notif.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                PriorityBadge(notif.priority)
            }
            if (notif.message.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(notif.message, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(notif.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                notif.tags.forEach { tag ->
                    Spacer(Modifier.weight(1f / (notif.tags.size + 1)))
                    AssistChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: Int) {
    val (label, color) = when (priority) {
        5 -> "MAX" to Color(0xFFE5484D)
        4 -> "HIGH" to Color(0xFFF76B15)
        1, 2 -> "LOW" to Color(0xFF6E7681)
        else -> null to Color.Transparent
    }
    if (label != null) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatTime(epochSeconds: Long): String {
    val dt = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())
    fun pad(n: Int) = n.toString().padStart(2, '0')
    return "${pad(dt.day)}/${pad(dt.month.number)}/${dt.year} ${pad(dt.hour)}:${pad(dt.minute)}"
}
