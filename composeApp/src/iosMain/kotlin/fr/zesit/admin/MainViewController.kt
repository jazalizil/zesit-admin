package fr.zesit.admin

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import fr.zesit.admin.di.AppContainer
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App(remember { AppContainer() }) }
