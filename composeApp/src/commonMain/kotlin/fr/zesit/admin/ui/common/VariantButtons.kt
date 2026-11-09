package fr.zesit.admin.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.zesit.admin.ui.theme.interactiveAccent

/**
 * Variantes visuelles des boutons outlined :
 *  - [NEUTRAL] : look Material par défaut (bordure outline, texte onSurface)
 *  - [ACCENT] : accent interactif — couleur secondaire en thème clair,
 *    primaire en thème sombre ([fr.zesit.admin.ui.theme.interactiveAccent])
 *  - [DANGER] : action destructive, texte et bordure en couleur d'erreur
 */
enum class OutlinedButtonVariant { NEUTRAL, ACCENT, DANGER }

/**
 * [OutlinedButton] avec variante : le variant teinte le contenu (texte +
 * icône) ET la bordure (paramètre `border` natif, distinct des couleurs).
 */
@Composable
fun VariantOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: OutlinedButtonVariant = OutlinedButtonVariant.NEUTRAL,
    content: @Composable RowScope.() -> Unit,
) {
    val accent = when (variant) {
        OutlinedButtonVariant.NEUTRAL -> null
        OutlinedButtonVariant.ACCENT -> interactiveAccent()
        OutlinedButtonVariant.DANGER -> MaterialTheme.colorScheme.error
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = if (accent != null) {
            BorderStroke(1.dp, if (enabled) accent else accent.copy(alpha = 0.4f))
        } else {
            ButtonDefaults.outlinedButtonBorder(enabled)
        },
        colors = if (accent != null) {
            ButtonDefaults.outlinedButtonColors(
                contentColor = accent,
                disabledContentColor = accent.copy(alpha = 0.4f),
            )
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        content = content,
    )
}
