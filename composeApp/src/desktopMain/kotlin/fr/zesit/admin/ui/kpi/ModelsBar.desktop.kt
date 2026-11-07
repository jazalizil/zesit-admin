package fr.zesit.admin.ui.kpi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.zesit.admin.viewmodel.KpiUiState

/**
 * Variante desktop de la barre des modèles enregistrés : délègue pour l'instant
 * à [DefaultModelsBar]. Remplacer ce corps pour un agencement desktop dédié.
 */
@Composable
actual fun ModelsBar(
    state: KpiUiState,
    onSelect: (String?) -> Unit,
    onLoad: () -> Unit,
    onSave: (name: String, description: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier,
) = DefaultModelsBar(state, onSelect, onLoad, onSave, onDelete, modifier)
