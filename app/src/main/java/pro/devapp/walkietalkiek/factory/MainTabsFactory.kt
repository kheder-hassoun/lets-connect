package pro.devapp.walkietalkiek.factory

import pro.devapp.walkietalkiek.R
import pro.devapp.walkietalkiek.model.MainTab
import pro.devapp.walkietalkiek.model.MainTabItem

class MainTabsFactory {

    fun createTabs(): List<MainTabItem> {
        return listOf(
            MainTabItem(
                titleRes = R.string.tab_settings,
                icon = R.drawable.settings,
                screen = MainTab.SETTINGS
            ),
            MainTabItem(
                titleRes = R.string.tab_chat,
                icon = R.drawable.chat,
                screen = MainTab.CHAT
            ),
            MainTabItem(
                titleRes = R.string.tab_ptt,
                icon = R.drawable.ptt,
                screen = MainTab.PTT
            ),
            MainTabItem(
                titleRes = R.string.tab_about,
                icon = R.drawable.about,
                screen = MainTab.ABOUT
            ),
            MainTabItem(
                titleRes = R.string.tab_status,
                icon = R.drawable.connection_on,
                screen = MainTab.OFF
            ),
        )
    }
}
