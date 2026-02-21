package pro.devapp.walkietalkiek.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.devapp.walkietalkiek.model.MainScreenAction
import pro.devapp.walkietalkiek.model.MainScreenState
import pro.devapp.walkietalkiek.model.MainTab

@Composable
fun RailTabs(
    modifier: Modifier = Modifier,
    screenState: MainScreenState,
    onAction: (MainScreenAction) -> Unit = {}
) {
    val accent = MaterialTheme.colorScheme.primary
    val accentAlt = MaterialTheme.colorScheme.tertiary
    val windowInsets = WindowInsets.navigationBars
    Surface(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 10.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        tonalElevation = 10.dp,
        shadowElevation = 12.dp
    ) {
        NavigationRail(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.14f),
                            accentAlt.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.26f),
                    shape = RoundedCornerShape(26.dp)
                ),
            windowInsets = windowInsets,
            containerColor = Color.Transparent
        ) {
            screenState.mainTabs.forEach {
                val isPtt = it.screen == MainTab.PTT
                val isSelected = it.screen == screenState.currentTab
                val pulseTransition = rememberInfiniteTransition(label = "ptt-rail")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(760),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ptt-rail-scale"
                )
                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        onAction(MainScreenAction.ChangeScreen(it.screen))
                    },
                    icon = {
                        if (isPtt) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) accent.copy(alpha = 0.28f) else Color(0x1EFFFFFF),
                                shadowElevation = if (isSelected) 10.dp else 3.dp,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) accent.copy(alpha = 0.65f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    painter = painterResource(it.icon),
                                    contentDescription = it.title,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(20.dp)
                                        .scale(if (isSelected) pulseScale else 1f)
                                )
                            }
                        } else {
                            Icon(
                                painter = painterResource(it.icon),
                                contentDescription = it.title,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = if (isPtt && isSelected) "PTT Live" else it.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        indicatorColor = accent.copy(alpha = 0.16f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                )
            }
        }
    }
}
