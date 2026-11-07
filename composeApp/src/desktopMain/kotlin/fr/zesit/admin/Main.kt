package fr.zesit.admin

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import fr.zesit.admin.di.AppContainer

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Zesit Admin",
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        App(remember { AppContainer() })
    }
}
