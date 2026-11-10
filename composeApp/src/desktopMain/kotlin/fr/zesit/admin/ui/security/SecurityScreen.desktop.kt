package fr.zesit.admin.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.SecurityViewModel

/**
 * Variante desktop de la section Sécurité : délègue à l'agencement commun
 * ([DefaultSecurityScreen]). Remplacer ce corps par un agencement desktop
 * dédié si besoin.
 */
@Composable
actual fun SecurityScreen(
    viewModel: SecurityViewModel,
    modifier: Modifier,
) = DefaultSecurityScreen(viewModel, modifier)
