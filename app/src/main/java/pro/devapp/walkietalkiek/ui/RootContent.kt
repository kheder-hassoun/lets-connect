package pro.devapp.walkietalkiek.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import org.koin.androidx.compose.getViewModel
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.MainViewMode
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.core.theme.DroidPTTTheme
import pro.devapp.walkietalkiek.model.MainScreenAction
import pro.devapp.walkietalkiek.model.MainScreenEvent
import pro.devapp.walkietalkiek.service.WalkieService
import pro.devapp.walkietalkiek.ui.components.BottomTabs
import pro.devapp.walkietalkiek.ui.components.MainTopBar
import pro.devapp.walkietalkiek.ui.components.RailTabs
import pro.devapp.walkietalkiek.ui.components.RequiredPermissionsNotification
import pro.devapp.walkietalkiek.ui.components.TabsContent

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun RootContent() {
    val viewModel: MainViewMode = getViewModel()
    val state = viewModel.state.collectAsState()
    val settingsRepository = koinInject<AppSettingsRepository>()
    val settings = settingsRepository.settings.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.onAction(MainScreenAction.InitApp)
    }

    DroidPTTTheme(themeColor = settings.value.themeColor) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MainTopBar(state = state.value)
            }
        ) { innerPadding ->
            val windowSize = with(LocalDensity.current) {
                currentWindowSize().toSize().toDpSize()
            }

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
                            BottomTabs(
                                screenState = state.value,
                                onAction = viewModel::onAction
                            )
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
                                colors = listOf(
                                    Color(0xFF050505),
                                    Color(0xFF0D0D0D),
                                    Color(0xFF050505)
                                )
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
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                context.startActivity(intent)
                            }
                        )
                    }
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
