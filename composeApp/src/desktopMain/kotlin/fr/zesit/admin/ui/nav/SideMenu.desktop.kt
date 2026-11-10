package fr.zesit.admin.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Largeur du panneau plié (rail d'icônes) et déplié. */
private val PanelWidthExpanded = 280.dp
private val PanelWidthCollapsed = 92.dp

/**
 * Menu latéral desktop — inspiration « Ring Tracker — Settings Dashboard »
 * (dribbble.com/shots/27335762) : panneau flottant arrondi, en-tête avec logo,
 * items pilules sur pastille d'icône (icône + titre + sous-titre), réglages
 * épinglés en bas. Le fond de la fenêtre (couleur de marque) reste visible
 * autour du panneau.
 *
 * Le menu se plie/déplie ([expanded] + [onToggleExpanded], chevrons en haut) :
 * la largeur s'anime entre le rail d'icônes ([PanelWidthCollapsed]) et le
 * panneau complet ([PanelWidthExpanded]) ; les libellés fondent au fil de la
 * transition et le contenu est rogné au rayon du panneau.
 */
@Composable
internal fun SideMenu(
    selected: DashboardSection,
    settingsOpen: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectSection: (DashboardSection) -> Unit,
    onToggleSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    // Transition de largeur (plier/déplier) ; les libellés fondent un peu plus vite.
    val panelWidth by animateDpAsState(
        targetValue = if (expanded) PanelWidthExpanded else PanelWidthCollapsed,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "panelWidth",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "labelAlpha",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(panelWidth)
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surface, RoundedCornerShape(24.dp))
            .border(BorderStroke(1.dp, scheme.outlineVariant), RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        // Plier / déplier : chevron à droite déplié, centré plié.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (expanded) Arrangement.End else Arrangement.Center,
        ) {
            IconButton(onClick = onToggleExpanded) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardDoubleArrowLeft else Icons.Default.KeyboardDoubleArrowRight,
                    contentDescription = if (expanded) "Plier le menu" else "Déplier le menu",
                    tint = scheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // En-tête : logo + nom de l'app (le nom fonde quand le menu se plie).
        // Textes sur une seule ligne : à largeur réduite, le wrap changerait la
        // hauteur de la rangée et décalerait verticalement tous les items en
        // dessous. Plié, le logo glisse au centre horizontal du rail
        // (translation animée en même temps que le fondu des libellés).
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Décalage du logo vers le centre du rail plié (0 déplié).
            Box(

                modifier = Modifier
                    .size(58.dp)
                    .padding(10.dp)
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
            Column(Modifier.graphicsLayer { alpha = labelAlpha }) {
                Text(
                    "Zesit Admin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    "Tableau de bord",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "MENU",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha },
        )
        Spacer(Modifier.height(10.dp))

        val dsIterator = DashboardSection.entries.iterator()

        DashboardSection.entries.forEach {
            SideMenuItem(
                icon = it.icon,
                // Libellé long sur desktop (menu large) — compact (label) côté mobile.
                label = it.longLabel,
                caption = it.description,
                selected = selected == it && !settingsOpen,
                expanded = expanded,
                labelAlpha = labelAlpha,
                onClick = { onSelectSection(it) },
            )
            if (dsIterator.hasNext()) {
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = scheme.outlineVariant)
        Spacer(Modifier.height(10.dp))
        SideMenuItem(
            icon = Icons.Default.Settings,
            label = "Réglages",
            caption = "Apparence, données, version",
            selected = settingsOpen,
            labelAlpha = labelAlpha,
            onClick = onToggleSettings,
            expanded = expanded,
        )
    }
}

/**
 * Item du menu : icône dans une pastille arrondie suivie du titre et d'un
 * sous-titre. Item sélectionné en pilule contrastée — crème (couleur claire)
 * sur le panneau brun du thème sombre, brun (couleur foncée) sur le panneau
 * crème du thème clair (rôles inverseSurface/inverseOnSurface), avec pastille
 * d'icône teintée. Plié, seul la pastille d'icône reste visible (libellés
 * fondus à [labelAlpha], contenu rogné par le panneau).
 */
@Composable
private fun SideMenuItem(
    icon: ImageVector,
    label: String,
    caption: String,
    selected: Boolean,
    labelAlpha: Float,
    onClick: () -> Unit,
    expanded: Boolean
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
            .background(container, RoundedCornerShape(16.dp))
            .hoverable(hoverInteraction)
            .clickable(interactionSource = hoverInteraction, indication = null) { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
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
        Column(Modifier.graphicsLayer { alpha = labelAlpha }) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) selectedContent else scheme.onSurface,
                maxLines = 1,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) selectedContent.copy(alpha = 0.75f)
                else scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
