package fr.zesit.admin.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Menu latéral desktop — inspiration « Ring Tracker — Settings Dashboard »
 * (dribbble.com/shots/27335762) : panneau flottant arrondi, en-tête avec logo,
 * items pilules sur pastille d'icône (icône + titre + sous-titre), réglages
 * épinglés en bas. Le fond de la fenêtre (couleur de marque) reste visible
 * autour du panneau.
 */
@Composable
internal fun SideMenu(
    selected: DashboardSection,
    settingsOpen: Boolean,
    onSelectSection: (DashboardSection) -> Unit,
    onToggleSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(scheme.surface, RoundedCornerShape(24.dp))
            .border(BorderStroke(1.dp, scheme.outlineVariant), RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        // En-tête : logo + nom de l'app
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(scheme.primary, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Z",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Zesit Admin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Tableau de bord",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "MENU",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        DashboardSection.entries.forEach { dest ->
            SideMenuItem(
                icon = dest.icon,
                label = dest.label,
                caption = dest.description,
                selected = selected == dest && !settingsOpen,
                onClick = { onSelectSection(dest) },
            )
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = scheme.outlineVariant)
        Spacer(Modifier.height(10.dp))
        SideMenuItem(
            icon = Icons.Default.Settings,
            label = "Réglages",
            caption = "Apparence, données, version",
            selected = settingsOpen,
            onClick = onToggleSettings,
        )
    }
}

/**
 * Item du menu : icône dans une pastille arrondie suivie du titre et d'un
 * sous-titre. Item sélectionné en pilule contrastée — crème (couleur claire)
 * sur le panneau brun du thème sombre, brun (couleur foncée) sur le panneau
 * crème du thème clair (rôles inverseSurface/inverseOnSurface), avec
 * pastille d'icône teintée.
 */
@Composable
private fun SideMenuItem(
    icon: ImageVector,
    label: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val selectedContainer = scheme.inverseSurface
    val selectedContent = scheme.inverseOnSurface

    val hoverInteraction = remember { MutableInteractionSource() }
    val hovered by hoverInteraction.collectIsHoveredAsState()

    val container by animateColorAsState(
        when {
            selected -> selectedContainer
            hovered -> scheme.onSurface.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        label = "itemContainer",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(vertical = 2.dp)
            .background(container, RoundedCornerShape(16.dp))
            .hoverable(hoverInteraction)
            .clickable(interactionSource = hoverInteraction, indication = null) { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (selected) selectedContent.copy(alpha = 0.16f)
                    else scheme.onSurface.copy(alpha = 0.07f),
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (selected) selectedContent else scheme.onSurface,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) selectedContent else scheme.onSurface,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) selectedContent.copy(alpha = 0.75f)
                else scheme.onSurfaceVariant,
            )
        }
    }
}
