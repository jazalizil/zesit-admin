package fr.zesit.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Palette Zesit Admin (4 couleurs de marque) :
//  - Primaire   : 230,207,148 (#E6CF94) — or
//  - Secondaire : 151,126,109 (#977E6D) — bronze
//  - Claire     : 252,247,227 (#FCF7E3) — crème
//  - Foncée     :  87, 72, 59 (#57483B) — brun
// Le fond de l'app reprend la couleur primaire en thème clair et la
// couleur secondaire en thème sombre ; la couleur claire sert de surface
// (cartes, panneau du menu) et de texte en thème sombre, la couleur foncée
// sert de texte en thème clair et de surface en thème sombre.
private val Primary = Color(0xFFE6CF94)   // 230,207,148
private val Secondary = Color(0xFF977E6D) // 151,126,109
private val Light = Color(0xFFFCF7E3)     // 252,247,227
private val Dark = Color(0xFF57483B)      // 87,72,59

// Border radius (storefront tailwind) : soft 2px, base 4px, rounded 8px, large 16px
private val ZesitShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// Thème clair : fond = primaire (or), cartes crème, texte brun foncé
private val ZesitLightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Dark,
    primaryContainer = Light,
    onPrimaryContainer = Dark,
    secondary = Secondary,
    onSecondary = Light,
    secondaryContainer = Color(0xFFEFE7D2),
    onSecondaryContainer = Dark,
    tertiary = Secondary,
    onTertiary = Light,
    background = Primary,
    onBackground = Dark,
    surface = Light,
    onSurface = Dark,
    surfaceVariant = Color(0xFFF6EFD9),
    onSurfaceVariant = Color(0xFF82705F),
    surfaceContainerLowest = Color(0xFFFFFDF1),
    surfaceContainerLow = Color(0xFFFDF8E7),
    surfaceContainer = Light,
    surfaceContainerHigh = Color(0xFFF5EEDA),
    surfaceContainerHighest = Color(0xFFEFE6C8),
    surfaceDim = Color(0xFFDFCCA0),
    surfaceBright = Color(0xFFFFF8DB),
    outline = Color(0xFFB8A175),
    outlineVariant = Color(0xFFE3D7B4),
    error = Color(0xFF9E2B26),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DED8),
    onErrorContainer = Color(0xFF4B0F0C),
    inverseSurface = Dark,
    inverseOnSurface = Light,
    inversePrimary = Primary,
    surfaceTint = Primary,
    scrim = Dark,
)

// Thème sombre : fond = secondaire (bronze), cartes brun foncé, texte crème
private val ZesitDarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = Dark,
    primaryContainer = Dark,
    onPrimaryContainer = Light,
    secondary = Secondary,
    onSecondary = Dark,
    secondaryContainer = Color(0xFF4C3F34),
    onSecondaryContainer = Light,
    tertiary = Secondary,
    onTertiary = Dark,
    background = Secondary,
    onBackground = Light,
    surface = Dark,
    onSurface = Light,
    surfaceVariant = Color(0xFF605041),
    onSurfaceVariant = Color(0xFFE2D8C5),
    surfaceContainerLowest = Color(0xFF46382F),
    surfaceContainerLow = Color(0xFF4C3F34),
    surfaceContainer = Dark,
    surfaceContainerHigh = Color(0xFF5F5142),
    surfaceContainerHighest = Color(0xFF685949),
    surfaceDim = Color(0xFF39362E),
    surfaceBright = Color(0xFF8B7666),
    outline = Color(0xFFB29781),
    outlineVariant = Color(0xFF6B5B4A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5C1613),
    errorContainer = Color(0xFF5C1613),
    onErrorContainer = Color(0xFFFFB4AB),
    inverseSurface = Light,
    inverseOnSurface = Dark,
    inversePrimary = Primary,
    surfaceTint = Primary,
    scrim = Color(0xFF2C241C),
)

@Composable
fun ZesitAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Sélection de texte : fond or (primaire), poignée brun foncé
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = Dark,
            backgroundColor = Primary,
        )
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) ZesitDarkColors else ZesitLightColors,
            shapes = ZesitShapes,
            content = content,
        )
    }
}
