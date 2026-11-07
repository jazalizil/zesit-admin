package fr.zesit.admin

import androidx.compose.runtime.Composable
import fr.zesit.admin.di.AppContainer

/** Actual iOS de la racine de l'app : agencement commun ([DefaultApp]). */
@Composable
actual fun App(container: AppContainer) = DefaultApp(container)
