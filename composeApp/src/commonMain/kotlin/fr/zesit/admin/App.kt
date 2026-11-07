package fr.zesit.admin

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.zesit.admin.bridge.ApnsBridge
import fr.zesit.admin.bridge.ApnsTokenHandler
import fr.zesit.admin.di.AppContainer
import fr.zesit.admin.ui.notifications.NotificationsScreen
import fr.zesit.admin.ui.settings.SettingsScreen
import fr.zesit.admin.ui.theme.ZesitAdminTheme
import fr.zesit.admin.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(container: AppContainer = remember { AppContainer() }) {
    ZesitAdminTheme {
        val viewModel: NotificationsViewModel = viewModel { NotificationsViewModel(container) }

        // Branche le pont APNs : l'AppDelegate Swift pousse le device token ici (iOS uniquement)
        DisposableEffect(Unit) {
            ApnsBridge.setTokenHandler(ApnsTokenHandler { tokenHex -> viewModel.onApnsDeviceToken(tokenHex) })
            onDispose { ApnsBridge.clearTokenHandler() }
        }

        var showSettings by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (showSettings) "Réglages" else "Zesit Admin") },
                    actions = {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Réglages",
                            )
                        }
                    },
                )
            },
        ) { padding ->
            if (showSettings) {
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding),
                    onBack = { showSettings = false },
                )
            } else {
                NotificationsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}
