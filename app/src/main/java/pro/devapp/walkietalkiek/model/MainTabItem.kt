package pro.devapp.walkietalkiek.model

import androidx.annotation.StringRes

data class MainTabItem(
    @StringRes val titleRes: Int,
    val icon: Int,
    val screen: MainTab
)
