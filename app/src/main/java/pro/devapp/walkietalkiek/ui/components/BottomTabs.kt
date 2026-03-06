package pro.devapp.walkietalkiek.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pro.devapp.walkietalkiek.R
import pro.devapp.walkietalkiek.model.MainScreenAction
import pro.devapp.walkietalkiek.model.MainScreenState
import pro.devapp.walkietalkiek.model.MainTab

@Composable
fun BottomTabs(
    modifier: Modifier = Modifier,
    screenState: MainScreenState,
    onAction: (MainScreenAction) -> Unit = {}
) {
    val accent = MaterialTheme.colorScheme.primary
    val accentAlt = MaterialTheme.colorScheme.tertiary
    val indicatorColor = accent.copy(alpha = 0.16f)

    val view = LocalView.current
    val density = LocalDensity.current
    val bottomInsetPx = ViewCompat.getRootWindowInsets(view)
        ?.getInsets(WindowInsetsCompat.Type.navigationBars())
        ?.bottom
        ?: 0
    val bottomInset = with(density) { bottomInsetPx.toDp() }

    val pttTab = screenState.mainTabs.firstOrNull { it.screen == MainTab.PTT }
    val pttIndex = screenState.mainTabs.indexOfFirst { it.screen == MainTab.PTT }
    val nonPttTabs = screenState.mainTabs.filterNot { it.screen == MainTab.PTT }
    val leftTabs = if (pttIndex >= 0) {
        screenState.mainTabs.take(pttIndex).filterNot { it.screen == MainTab.PTT }
    } else {
        nonPttTabs
    }
    val rightTabs = if (pttIndex >= 0) {
        screenState.mainTabs.drop(pttIndex + 1).filterNot { it.screen == MainTab.PTT }
    } else {
        emptyList()
    }
    val isPttSelected = screenState.currentTab == MainTab.PTT
    val pttFabSize = 60.dp
    val pttBarGap = 68.dp

    val pulseTransition = rememberInfiniteTransition(label = "ptt-fab")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(760),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ptt-fab-scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = bottomInset + 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
            tonalElevation = 10.dp,
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.14f),
                                accentAlt.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.26f),
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    leftTabs.forEach { tab ->
                        BottomTabItem(
                            tab = tab,
                            isSelected = tab.screen == screenState.currentTab,
                            accent = accent,
                            indicatorColor = indicatorColor,
                            onAction = onAction
                        )
                    }

                    if (pttTab != null) {
                        Spacer(modifier = Modifier.width(pttBarGap))
                    }

                    rightTabs.forEach { tab ->
                        BottomTabItem(
                            tab = tab,
                            isSelected = tab.screen == screenState.currentTab,
                            accent = accent,
                            indicatorColor = indicatorColor,
                            onAction = onAction
                        )
                    }
                }
            }
        }

        if (pttTab != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(2f),
                shape = CircleShape,
                color = if (isPttSelected) {
                    accent
                } else {
                    MaterialTheme.colorScheme.surface
                },
                tonalElevation = 14.dp,
                shadowElevation = 14.dp
            ) {
                Box(
                    modifier = Modifier
                        .size(pttFabSize)
                        .border(
                            width = 1.4.dp,
                            color = if (isPttSelected) {
                                accent.copy(alpha = 0.78f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
                            },
                            shape = CircleShape
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isPttSelected) {
                                    listOf(
                                        accent.copy(alpha = 0.92f),
                                        accentAlt.copy(alpha = 0.78f)
                                    )
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                    )
                                }
                            ),
                            shape = CircleShape
                        )
                        .padding(14.dp)
                ) {
                    Icon(
                        painter = painterResource(pttTab.icon),
                        contentDescription = stringResource(pttTab.titleRes),
                        tint = if (isPttSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .matchParentSize()
                            .scale(if (isPttSelected) pulseScale else 1f)
                    )
                }
            }

            Text(
                text = stringResource(R.string.tab_talk),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isPttSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isPttSelected) {
                    accent
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = pttFabSize + 6.dp)
                    .zIndex(3f)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(pttFabSize)
                    .zIndex(3f)
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        onAction(MainScreenAction.ChangeScreen(MainTab.PTT))
                    }
            )
        }
    }
}

@Composable
private fun RowScope.BottomTabItem(
    tab: pro.devapp.walkietalkiek.model.MainTabItem,
    isSelected: Boolean,
    accent: Color,
    indicatorColor: Color,
    onAction: (MainScreenAction) -> Unit
) {
    NavigationBarItem(
        selected = isSelected,
        alwaysShowLabel = true,
        onClick = {
            onAction(MainScreenAction.ChangeScreen(tab.screen))
        },
        icon = {
            Icon(
                painter = painterResource(tab.icon),
                contentDescription = stringResource(tab.titleRes),
                modifier = Modifier.size(20.dp)
            )
        },
        label = {
            Text(
                text = stringResource(tab.titleRes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = accent,
            selectedTextColor = accent,
            indicatorColor = indicatorColor,
            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
    )
}
