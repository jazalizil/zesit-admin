package fr.zesit.admin.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Accent interactif : la couleur secondaire (bronze) en thème clair — pour
 * trancher sur le fond doré et les cartes crème — et la couleur primaire (or)
 * en thème sombre. Appliqué au focus des champs, aux fonds de boutons pleins
 * et aux tracks de sliders.
 */
@Composable
fun interactiveAccent(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() >= 0.5f) scheme.secondary else scheme.primary
}

/** Contenu (texte/icône) posé sur [interactiveAccent]. */
@Composable
fun onInteractiveAccent(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() >= 0.5f) scheme.onSecondary else scheme.onPrimary
}

/** Couleurs de champ (bordure focus = accent, piste/curseur assortis). */
@Composable
fun fieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = interactiveAccent(),
        focusedLabelColor = interactiveAccent(),
        cursorColor = interactiveAccent(),
    )

/** Couleurs de bouton plein (fond = accent). */
@Composable
fun filledButtonColors(): ButtonColors =
    ButtonDefaults.buttonColors(
        containerColor = interactiveAccent(),
        contentColor = onInteractiveAccent(),
    )

/** Couleurs de slider : tracks (active + inactive) et pouce en accent. */
@Composable
fun sliderColors(): SliderColors =
    SliderDefaults.colors(
        activeTrackColor = interactiveAccent(),
        inactiveTrackColor = interactiveAccent().copy(alpha = 0.25f),
        thumbColor = interactiveAccent(),
    )
