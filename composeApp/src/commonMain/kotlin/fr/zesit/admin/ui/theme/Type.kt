package fr.zesit.admin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import fr.zesit.admin.Res
import fr.zesit.admin.montserrat_bold
import fr.zesit.admin.montserrat_medium
import fr.zesit.admin.montserrat_regular
import fr.zesit.admin.montserrat_semibold
import org.jetbrains.compose.resources.Font

/**
 * Montserrat, police par défaut de l'app sur toutes les plateformes
 * (desktop/Android/iOS) — 4 graisses embarquées dans les ressources Compose
 * (subset latin : accents français couverts), choisies sur les usages réels
 * du code : Regular pour le corps, Medium pour les titres/labels M3,
 * SemiBold (le plus demandé) et Bold pour les emphases explicites.
 *
 * Tout ce qui n'impose pas sa police (Text sans fontFamily) tombe dessus via
 * [montserratTypography] ; le journal du Traffic, explicitement
 * `FontFamily.Monospace`, garde sa fonte mono.
 */
@Composable
private fun montserratFontFamily(): FontFamily = FontFamily(
    Font(Res.font.montserrat_regular, FontWeight.Normal),
    Font(Res.font.montserrat_medium, FontWeight.Medium),
    Font(Res.font.montserrat_semibold, FontWeight.SemiBold),
    Font(Res.font.montserrat_bold, FontWeight.Bold),
)

/**
 * [androidx.compose.material3.Typography] M3 par défaut, reportée sur
 * Montserrat : chaque style garde tailles/interlignages/letterSpacings du
 * Material 3, seule la famille change.
 */
@Composable
fun montserratTypography(): Typography {
    val default = Typography()
    val montserrat = montserratFontFamily()
    return Typography(
        displayLarge = default.displayLarge.copy(fontFamily = montserrat),
        displayMedium = default.displayMedium.copy(fontFamily = montserrat),
        displaySmall = default.displaySmall.copy(fontFamily = montserrat),
        headlineLarge = default.headlineLarge.copy(fontFamily = montserrat),
        headlineMedium = default.headlineMedium.copy(fontFamily = montserrat),
        headlineSmall = default.headlineSmall.copy(fontFamily = montserrat),
        titleLarge = default.titleLarge.copy(fontFamily = montserrat),
        titleMedium = default.titleMedium.copy(fontFamily = montserrat),
        titleSmall = default.titleSmall.copy(fontFamily = montserrat),
        bodyLarge = default.bodyLarge.copy(fontFamily = montserrat),
        bodyMedium = default.bodyMedium.copy(fontFamily = montserrat),
        bodySmall = default.bodySmall.copy(fontFamily = montserrat),
        labelLarge = default.labelLarge.copy(fontFamily = montserrat),
        labelMedium = default.labelMedium.copy(fontFamily = montserrat),
        labelSmall = default.labelSmall.copy(fontFamily = montserrat),
    )
}
