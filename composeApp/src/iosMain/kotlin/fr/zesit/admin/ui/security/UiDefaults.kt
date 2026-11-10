package fr.zesit.admin.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.SecurityViewModel

/** Actual iOS de la section Sécurité : agencement commun ([DefaultSecurityScreen]). */
@Composable
actual fun SecurityScreen(
    viewModel: SecurityViewModel,
    modifier: Modifier,
) = DefaultSecurityScreen(viewModel, modifier)
