package pro.devapp.walkietalkiek.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pro.devapp.walkietalkiek.model.MainScreenAction
import pro.devapp.walkietalkiek.model.MainScreenState

@Composable
fun RailTabs(
    modifier: Modifier = Modifier,
    screenState: MainScreenState,
    onAction: (MainScreenAction) -> Unit = {}
) {
    val windowInsets = WindowInsets.navigationBars
    Surface(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xE6161616),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp
    ) {
        NavigationRail(
            modifier = Modifier,
            windowInsets = windowInsets,
            containerColor = Color.Transparent
        ) {
            screenState.mainTabs.forEach {
                NavigationRailItem(
                    selected = it.screen == screenState.currentTab,
                    onClick = {
                        onAction(
                            MainScreenAction.ChangeScreen(
                                it.screen
                            )
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(it.icon),
                            contentDescription = it.title
                        )
                    },
                    label = {
                        Text(
                            text = it.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFA726),
                        selectedTextColor = Color(0xFFFFA726),
                        indicatorColor = Color(0x26FF8A00),
                        unselectedIconColor = Color(0xFF9E9E9E),
                        unselectedTextColor = Color(0xFF9E9E9E)
                    )
                )
            }
        }
    }
}
