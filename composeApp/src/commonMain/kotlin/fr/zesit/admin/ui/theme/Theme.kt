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

// Palette ZESIT — source unique : apps/storefront/src/styles/globals.css (:root)
// + grey scale de apps/storefront/tailwind.config.js
private val LemonLight = Color(0xFFFFF3B0) // --color-lemon-light
private val Lemon = Color(0xFFFFD400)      // --color-lemon
private val LemonDark = Color(0xFFE6BE00)  // --color-lemon-dark
private val Ink = Color(0xFF1B1810)        // --color-ink
private val Cream = Color(0xFFFFFCF2)      // --color-cream
private val Leaf = Color(0xFF8DC63F)       // --color-leaf
private val Grey10 = Color(0xFFF3F4F6)
private val Grey20 = Color(0xFFE5E7EB)
private val Grey30 = Color(0xFFD1D5DB)
private val Grey60 = Color(0xFF4B5563)
private val Grey70 = Color(0xFF374151)
private val Grey80 = Color(0xFF1F2937)

// Thème clair = storefront (fond cream, texte ink, interactif lemon/ink :
// --bg-interactive: lemon, --fg-interactive: ink, hover lemon-dark)
private val ZesitLightColors = lightColorScheme(
    primary = Lemon,
    onPrimary = Ink,
    primaryContainer = LemonLight,
    onPrimaryContainer = Ink,
    secondary = Leaf,
    onSecondary = Ink,
    secondaryContainer = LemonLight,
    onSecondaryContainer = Ink,
    tertiary = LemonDark,
    onTertiary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Grey10,
    onSurfaceVariant = Grey60,
    outline = Grey30,
    outlineVariant = Grey20,
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    inverseSurface = Ink,
    inverseOnSurface = Cream,
    inversePrimary = LemonDark,
    surfaceTint = Lemon,
    scrim = Ink,
)

// Variante sombre dérivée de la même palette (le storefront n'a pas de thème .dark custom)
private val ZesitDarkColors = darkColorScheme(
    primary = Lemon,
    onPrimary = Ink,
    primaryContainer = LemonDark,
    onPrimaryContainer = Ink,
    secondary = Leaf,
    onSecondary = Ink,
    background = Ink,
    onBackground = Cream,
    surface = Grey80,
    onSurface = Cream,
    surfaceVariant = Grey70,
    onSurfaceVariant = Grey30,
    outline = Grey60,
    outlineVariant = Grey70,
    inverseSurface = Cream,
    inverseOnSurface = Ink,
    inversePrimary = Lemon,
    surfaceTint = Lemon,
    scrim = Ink,
)

// Border radius du storefront (tailwind borderRadius) : soft 2px, base 4px, rounded 8px, large 16px
private val ZesitShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ZesitAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // ::selection du storefront : fond lemon, texte ink
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = Ink,
            backgroundColor = Lemon,
        )
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) ZesitDarkColors else ZesitLightColors,
            shapes = ZesitShapes,
            content = content,
        )
    }
}
