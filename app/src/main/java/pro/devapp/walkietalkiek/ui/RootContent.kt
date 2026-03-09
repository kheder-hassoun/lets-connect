package pro.devapp.walkietalkiek.ui

import android.app.Activity
import android.media.AudioManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import androidx.core.view.WindowCompat
import org.koin.androidx.compose.getViewModel
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.MainViewMode
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.core.settings.ThemeMode
import pro.devapp.walkietalkiek.core.theme.DroidPTTTheme
import pro.devapp.walkietalkiek.core.theme.rememberBoundedDensity
import pro.devapp.walkietalkiek.core.theme.rememberResponsiveTypography
import pro.devapp.walkietalkiek.model.MainScreenAction
import pro.devapp.walkietalkiek.model.MainScreenEvent
import pro.devapp.walkietalkiek.model.MainTab
import pro.devapp.walkietalkiek.service.WalkieService
import pro.devapp.walkietalkiek.localization.AppLocaleManager
import pro.devapp.walkietalkiek.ui.components.BottomTabs
import pro.devapp.walkietalkiek.ui.components.CALL_VOLUME_MIN_PERCENT
import pro.devapp.walkietalkiek.ui.components.LowCallVolumeOverlay
import pro.devapp.walkietalkiek.ui.components.MainTopBar
import pro.devapp.walkietalkiek.ui.components.RailTabs
import pro.devapp.walkietalkiek.ui.components.RequiredPermissionsNotification
import pro.devapp.walkietalkiek.ui.components.TabsContent
import pro.devapp.walkietalkiek.ui.components.WelcomeTutorialOverlay
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun RootContent() {
    val viewModel: MainViewMode = getViewModel()
    val state = viewModel.state.collectAsState()
    val settingsRepository = koinInject<AppSettingsRepository>()
    val settings = settingsRepository.settings.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val isDarkTheme = when (settings.value.themeMode) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    var showWelcomeTutorial by rememberSaveable(settings.value.showWelcomeTutorial) {
        mutableStateOf(settings.value.showWelcomeTutorial)
    }
    val tutorialVisible = showWelcomeTutorial && settings.value.showWelcomeTutorial
    val appBlur = if (tutorialVisible) 12.dp else 0.dp

    val context = LocalContext.current
    val callVolumePercent = rememberCallVolumePercent(context)
    val isLowCallVolume = callVolumePercent in 0 until CALL_VOLUME_MIN_PERCENT
    var lowVolumeWarningDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isLowCallVolume) {
        if (!isLowCallVolume) {
            lowVolumeWarningDismissed = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onAction(MainScreenAction.InitApp)
    }

    LaunchedEffect(settings.value.appLanguage) {
        val didChange = AppLocaleManager.applyLanguage(
            context = context,
            language = settings.value.appLanguage
        )
        if (didChange) {
            context.findActivity()?.recreate()
        }
    }

    DroidPTTTheme(
        darkTheme = isDarkTheme,
        themeColor = settings.value.themeColor,
        typography = rememberResponsiveTypography()
    ) {
        CompositionLocalProvider(LocalDensity provides rememberBoundedDensity()) {
            SyncSystemBarsAppearance(isDarkTheme = isDarkTheme)
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(appBlur),
                    topBar = {
                        MainTopBar(
                            state = state.value
                        )
                    }
                ) { innerPadding ->
                    val windowSize = with(LocalDensity.current) {
                        currentWindowSize().toSize().toDpSize()
                    }
                    val imeVisible = rememberKeyboardVisible()
                    val hideBottomTabsForChatIme =
                        state.value.currentTab == MainTab.CHAT && imeVisible

                    val navLayoutType = if (windowSize.width > windowSize.height) {
                        // Landscape mode
                        NavigationSuiteType.NavigationRail
                    } else {
                        // Portrait mode
                        NavigationSuiteType.NavigationBar
                    }

                    NavigationSuiteScaffoldLayout(
                        layoutType = navLayoutType,
                        navigationSuite = {
                            when (navLayoutType) {
                                NavigationSuiteType.NavigationBar -> {
                                    if (!hideBottomTabsForChatIme) {
                                        BottomTabs(
                                            screenState = state.value,
                                            onAction = viewModel::onAction
                                        )
                                    }
                                }

                                NavigationSuiteType.NavigationRail -> {
                                    RailTabs(
                                        modifier = Modifier
                                            .padding(innerPadding),
                                        screenState = state.value,
                                        onAction = viewModel::onAction
                                    )
                                }

                                NavigationSuiteType.NavigationDrawer -> {

                                }
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isDarkTheme) {
                                            listOf(
                                                MaterialTheme.colorScheme.background,
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.background
                                            )
                                        } else {
                                            listOf(
                                                MaterialTheme.colorScheme.background,
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.background
                                            )
                                        }
                                    )
                                )
                                .padding(innerPadding)
                        ) {
                            TabsContent(
                                state = state.value,
                                onAction = viewModel::onAction
                            )
                            if (state.value.requiredPermissions.isNotEmpty()) {
                                RequiredPermissionsNotification(
                                    requiredPermissions = state.value.requiredPermissions,
                                    onClick = {
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data =
                                                    Uri.fromParts("package", context.packageName, null)
                                            }
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }

                if (tutorialVisible) {
                    WelcomeTutorialOverlay(
                        onFinish = { neverShowAgain ->
                            showWelcomeTutorial = false
                            if (neverShowAgain) {
                                settingsRepository.updateShowWelcomeTutorial(false)
                            }
                        }
                    )
                } else if (isLowCallVolume && !lowVolumeWarningDismissed) {
                    LowCallVolumeOverlay(
                        currentPercent = callVolumePercent,
                        onOkClick = { lowVolumeWarningDismissed = true }
                    )
                }
            }
        }
    }

    state.value.requiredPermissions.firstOrNull()?.let { permission ->
        val permissionsState = rememberPermissionState(
            permission
        ) {
            viewModel.onAction(MainScreenAction.CheckPermissions)
        }
        LaunchedEffect(permission) {
            permissionsState.launchPermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect {
            when (it) {
                is MainScreenEvent.RequestPermissions -> {

                }

                MainScreenEvent.StartService -> {
                    val serviceIntent = Intent(context, WalkieService::class.java)
                    context.startService(serviceIntent)
                }
            }
        }
    }

}

@Composable
private fun rememberKeyboardVisible(): Boolean {
    val view = LocalView.current
    val isKeyboardVisible = remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val rect = Rect()
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val keyboardRatio = keypadHeight.toFloat() / screenHeight.toFloat().coerceAtLeast(1f)
            val shouldShow = keyboardRatio > 0.15f
            val shouldHide = keyboardRatio < 0.10f

            when {
                !isKeyboardVisible.value && shouldShow -> isKeyboardVisible.value = true
                isKeyboardVisible.value && shouldHide -> isKeyboardVisible.value = false
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return isKeyboardVisible.value
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun rememberCallVolumePercent(context: Context): Int {
    val appContext = context.applicationContext
    val audioManager = remember {
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val percent by produceState(initialValue = 100, audioManager) {
        while (true) {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL).coerceAtLeast(1)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL).coerceAtLeast(0)
            value = ((current * 100f) / max).toInt().coerceIn(0, 100)
            delay(600)
        }
    }
    return percent
}

@Composable
private fun SyncSystemBarsAppearance(isDarkTheme: Boolean) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return
    SideEffect {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
}
