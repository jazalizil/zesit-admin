package com.zesit.admin

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
import com.zesit.admin.apns.ApnsBridge
import com.zesit.admin.di.AppContainer
import com.zesit.admin.ui.notifications.NotificationsScreen
import com.zesit.admin.ui.settings.SettingsScreen
import com.zesit.admin.ui.theme.ZesitAdminTheme
import com.zesit.admin.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(container: AppContainer = remember { AppContainer() }) {
    ZesitAdminTheme {
        val viewModel: NotificationsViewModel = viewModel { NotificationsViewModel(container) }

        // Branche le pont APNs : l'AppDelegate Swift pousse le device token ici (iOS uniquement)
        DisposableEffect(Unit) {
            ApnsBridge.handler = { tokenHex -> viewModel.onApnsDeviceToken(tokenHex) }
            onDispose { ApnsBridge.handler = null }
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
