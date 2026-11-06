package fr.zesit.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ZesitDarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF7DD3A8),
    secondary = androidx.compose.ui.graphics.Color(0xFF5B8DEF),
    background = androidx.compose.ui.graphics.Color(0xFF101418),
    surface = androidx.compose.ui.graphics.Color(0xFF1A2027),
)

@Composable
fun ZesitAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZesitDarkColors,
        content = content,
    )
}
