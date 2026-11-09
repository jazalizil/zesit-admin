package fr.zesit.admin

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import fr.zesit.admin.di.AppContainer

fun main() {
    // Suit l'apparence macOS (clair/sombre) pour isSystemInDarkTheme() (mode
    // Thème « Système » des Réglages) : sans cette propriété, la détection AWT
    // du JBR est indéterministe au lancement d'un process en arrière-plan.
    System.setProperty("apple.awt.application.appearance", "system")
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Zesit Admin",
            state = rememberWindowState(width = 1280.dp, height = 800.dp),
        ) {
            App(remember { AppContainer() })
        }
    }
}
