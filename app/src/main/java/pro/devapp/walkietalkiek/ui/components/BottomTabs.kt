package pro.devapp.walkietalkiek.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val selectedChipColor = accent.copy(alpha = 0.24f)
    val indicatorColor = accent.copy(alpha = 0.2f)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xE6161616),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            windowInsets = WindowInsets.navigationBars
        ) {
            screenState.mainTabs.forEach {
                val isPtt = it.screen == MainTab.PTT
                val isSelected = it.screen == screenState.currentTab
                val pulseTransition = rememberInfiniteTransition(label = "ptt-nav")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ptt-nav-scale"
                )
                NavigationBarItem(
                    selected = isSelected,
                    alwaysShowLabel = true,
                    onClick = {
                        onAction(
                            MainScreenAction.ChangeScreen(
                                it.screen
                            )
                        )
                    },
                    icon = {
                        if (isPtt) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) selectedChipColor else Color(0x1AFFFFFF),
                                shadowElevation = if (isSelected) 10.dp else 2.dp
                            ) {
                                Icon(
                                    painter = painterResource(it.icon),
                                    contentDescription = it.title,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .scale(if (isSelected) pulseScale else 1f)
                                )
                            }
                        } else {
                            Icon(
                                painter = painterResource(it.icon),
                                contentDescription = it.title
                            )
                        }
                    },
                    label = {
                        Text(
                            text = it.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        indicatorColor = indicatorColor,
                        unselectedIconColor = Color(0xFF9E9E9E),
                        unselectedTextColor = Color(0xFF9E9E9E)
                    )
                )
            }
        }
    }
}
