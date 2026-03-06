package pro.devapp.walkietalkiek.feature.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.core.diagnostics.DeviceLogStore
import pro.devapp.walkietalkiek.core.flags.FeatureFlagsRepository
import pro.devapp.walkietalkiek.core.settings.AppLanguage
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.core.settings.ThemeColor
import pro.devapp.walkietalkiek.core.settings.ThemeMode
import pro.devapp.walkietalkiek.serivce.network.data.ClusterMembershipRepository
import pro.devapp.walkietalkiek.serivce.network.data.ClusterRole
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SettingsContent() {
    val context = LocalContext.current
    val settingsRepository = koinInject<AppSettingsRepository>()
    val featureFlagsRepository = koinInject<FeatureFlagsRepository>()
    val clusterMembershipRepository = koinInject<ClusterMembershipRepository>()
    val deviceLogStore = koinInject<DeviceLogStore>()
    val settings by settingsRepository.settings.collectAsState()
    val flags by featureFlagsRepository.flags.collectAsState()
    val clusterStatus by clusterMembershipRepository.status.collectAsState()
    val isLeader = clusterStatus.role == ClusterRole.LEADER
    val isSystemTheme = settings.themeMode == ThemeMode.SYSTEM
    val isLightMode = settings.themeMode == ThemeMode.LIGHT
    var isThemeMenuExpanded by remember { mutableStateOf(false) }
    var isLanguageMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isLeader) {
                    Color(0x1A56E39F)
                } else {
                    Color(0x1AFFB74D)
                }
            )
        ) {
            Text(
                text = if (isLeader) {
                    stringResource(R.string.settings_role_leader)
                } else {
                    stringResource(R.string.settings_role_peer)
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (isLeader) Color(0xFF56E39F) else Color(0xFFFFB74D),
                fontWeight = FontWeight.SemiBold
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_talk_limit_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        R.string.settings_talk_limit_duration,
                        settings.talkDurationSeconds
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Slider(
                    value = settings.talkDurationSeconds.toFloat(),
                    onValueChange = { value ->
                        settingsRepository.updateTalkDurationSeconds(value.roundToInt())
                    },
                    valueRange = 5f..120f,
                    steps = 22,
                    enabled = isLeader
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_ptt_tones_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.toneEnabled,
                        onClick = { settingsRepository.updateToneEnabled(true) },
                        enabled = isLeader,
                        label = { Text(stringResource(R.string.common_on)) }
                    )
                    FilterChip(
                        selected = !settings.toneEnabled,
                        onClick = { settingsRepository.updateToneEnabled(false) },
                        enabled = isLeader,
                        label = { Text(stringResource(R.string.common_off)) }
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_theme_mode_title),
                    style = MaterialTheme.typography.titleMedium
                )
                FlagToggleRow(
                    title = stringResource(R.string.settings_use_system_theme),
                    checked = isSystemTheme,
                    onCheckedChange = { checked ->
                        settingsRepository.updateThemeMode(
                            if (checked) ThemeMode.SYSTEM else ThemeMode.DARK
                        )
                    },
                    enabled = true
                )
                if (!isSystemTheme) {
                    FlagToggleRow(
                        title = stringResource(R.string.settings_light_mode),
                        checked = isLightMode,
                        onCheckedChange = { checked ->
                            settingsRepository.updateThemeMode(
                                if (checked) ThemeMode.LIGHT else ThemeMode.DARK
                            )
                        },
                        enabled = true
                    )
                }
                Text(
                    text = stringResource(
                        R.string.settings_theme_mode_current,
                        themeModeDisplayName(settings.themeMode)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_language_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedLanguage = settings.appLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0x1A4DA3FF),
                                        Color(0x1A56E39F)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { isLanguageMenuExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = languageDisplayName(selectedLanguage),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (isLanguageMenuExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = stringResource(R.string.settings_select_language)
                        )
                    }

                    DropdownMenu(
                        expanded = isLanguageMenuExpanded,
                        onDismissRequest = { isLanguageMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.94f)
                    ) {
                        appLanguageOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = languageDisplayName(option)) },
                                onClick = {
                                    settingsRepository.updateAppLanguage(option)
                                    isLanguageMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_theme_color_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedTheme = themeOptionFor(settings.themeColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        selectedTheme.primary.copy(alpha = 0.24f),
                                        selectedTheme.accent.copy(alpha = 0.14f)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { isThemeMenuExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(selectedTheme.primary, CircleShape)
                            )
                            Text(
                                text = themeDisplayName(selectedTheme.themeColor),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (isThemeMenuExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = stringResource(R.string.settings_select_theme)
                        )
                    }

                    DropdownMenu(
                        expanded = isThemeMenuExpanded,
                        onDismissRequest = { isThemeMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.94f)
                    ) {
                        themeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(option.primary, CircleShape)
                                        )
                                        Text(text = themeDisplayName(option.themeColor))
                                    }
                                },
                                onClick = {
                                    settingsRepository.updateThemeColor(option.themeColor)
                                    isThemeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_advanced_flags_title),
                    style = MaterialTheme.typography.titleMedium
                )

                FlagToggleRow(
                    title = stringResource(R.string.settings_flag_serverless_control),
                    checked = flags.serverlessControl,
                    onCheckedChange = featureFlagsRepository::updateServerlessControl,
                    enabled = isLeader
                )
                FlagToggleRow(
                    title = stringResource(R.string.settings_flag_webrtc_audio),
                    checked = flags.webrtcAudio,
                    onCheckedChange = featureFlagsRepository::updateWebrtcAudio,
                    enabled = isLeader
                )
                FlagToggleRow(
                    title = stringResource(R.string.settings_flag_central_settings),
                    checked = flags.centralSettings,
                    onCheckedChange = featureFlagsRepository::updateCentralSettings,
                    enabled = isLeader
                )
                FlagToggleRow(
                    title = stringResource(R.string.settings_flag_floor_control_v2),
                    checked = flags.floorV2,
                    onCheckedChange = featureFlagsRepository::updateFloorV2,
                    enabled = isLeader
                )
                FlagToggleRow(
                    title = stringResource(R.string.settings_flag_observability_v2),
                    checked = flags.observabilityV2,
                    onCheckedChange = featureFlagsRepository::updateObservabilityV2,
                    enabled = isLeader
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_diagnostics_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        R.string.settings_logs_path,
                        deviceLogStore.logsDirectoryPath()
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                shareLogFile(
                                    context = context,
                                    file = deviceLogStore.appLogFile(),
                                    chooserTitle = context.getString(R.string.settings_share_app_log_chooser)
                                )
                            }
                        ) {
                            Text(stringResource(R.string.settings_share_app_log_button))
                        }
                        Button(
                            onClick = {
                                shareLogFile(
                                    context = context,
                                    file = deviceLogStore.crashLogFile(),
                                    chooserTitle = context.getString(R.string.settings_share_crash_log_chooser)
                                )
                            }
                        ) {
                            Text(stringResource(R.string.settings_share_crash_button))
                        }
                    }
                    Button(
                        onClick = {
                            deviceLogStore.clearLogs()
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_logs_cleared_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text(stringResource(R.string.settings_clear_logs_button))
                    }
                }
            }
        }

        Box(modifier = Modifier.size(2.dp))
    }
}

private fun shareLogFile(
    context: Context,
    file: File,
    chooserTitle: String
) {
    if (!file.exists() || file.length() == 0L) {
        return
    }
    val authority = "${context.packageName}.fileprovider"
    val fileUri = FileProvider.getUriForFile(context, authority, file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
}

@Composable
private fun FlagToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.52f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

private data class ThemeOptionUi(
    val themeColor: ThemeColor,
    val primary: Color,
    val accent: Color
)

private val themeOptions = listOf(
    ThemeOptionUi(ThemeColor.PURPLE, Color(0xFFA970FF), Color(0xFFC49BFF)),
    ThemeOptionUi(ThemeColor.BLUE, Color(0xFF4DA3FF), Color(0xFF82C4FF))
)

private fun themeOptionFor(themeColor: ThemeColor): ThemeOptionUi {
    return themeOptions.firstOrNull { it.themeColor == themeColor }
        ?: themeOptions.first()
}

private val appLanguageOptions = listOf(
    AppLanguage.ENGLISH,
    AppLanguage.ARABIC
)

@Composable
private fun languageDisplayName(language: AppLanguage): String {
    return when (language) {
        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
        AppLanguage.ARABIC -> stringResource(R.string.language_arabic)
    }
}

@Composable
private fun themeDisplayName(themeColor: ThemeColor): String {
    return when (themeColor) {
        ThemeColor.PURPLE -> stringResource(R.string.theme_purple)
        ThemeColor.BLUE -> stringResource(R.string.theme_blue)
    }
}

@Composable
private fun themeModeDisplayName(themeMode: ThemeMode): String {
    return when (themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
        ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
    }
}
