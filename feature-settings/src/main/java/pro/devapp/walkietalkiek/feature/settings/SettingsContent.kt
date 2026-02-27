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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.core.diagnostics.DeviceLogStore
import pro.devapp.walkietalkiek.core.flags.FeatureFlagsRepository
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.core.settings.ThemeColor
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SettingsContent() {
    val context = LocalContext.current
    val settingsRepository = koinInject<AppSettingsRepository>()
    val featureFlagsRepository = koinInject<FeatureFlagsRepository>()
    val deviceLogStore = koinInject<DeviceLogStore>()
    val settings by settingsRepository.settings.collectAsState()
    val flags by featureFlagsRepository.flags.collectAsState()
    var isThemeMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                    text = "Talk Timer",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Max speaking time: ${settings.talkDurationSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Slider(
                    value = settings.talkDurationSeconds.toFloat(),
                    onValueChange = { value ->
                        settingsRepository.updateTalkDurationSeconds(value.roundToInt())
                    },
                    valueRange = 5f..120f,
                    steps = 22
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
                    text = "PTT Tone",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.toneEnabled,
                        onClick = { settingsRepository.updateToneEnabled(true) },
                        label = { Text("On") }
                    )
                    FilterChip(
                        selected = !settings.toneEnabled,
                        onClick = { settingsRepository.updateToneEnabled(false) },
                        label = { Text("Off") }
                    )
                }
                Text(
                    text = "Plays tone before transmit/receive when On",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                    text = "Theme Color",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Background is always black. Choose accent color:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
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
                                text = selectedTheme.themeColor.title,
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
                            contentDescription = "Select theme"
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
                                        Text(text = option.themeColor.title)
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
                    text = "Feature Flags",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Runtime toggles for phased rollout (debug/ops use).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                FlagToggleRow(
                    title = "Serverless Control",
                    description = "Enable in-cluster leader/membership control path.",
                    checked = flags.serverlessControl,
                    onCheckedChange = featureFlagsRepository::updateServerlessControl
                )
                FlagToggleRow(
                    title = "WebRTC Audio",
                    description = "Use WebRTC media path for group audio.",
                    checked = flags.webrtcAudio,
                    onCheckedChange = featureFlagsRepository::updateWebrtcAudio
                )
                FlagToggleRow(
                    title = "Central Settings",
                    description = "Apply coordinator-driven settings sync.",
                    checked = flags.centralSettings,
                    onCheckedChange = featureFlagsRepository::updateCentralSettings
                )
                FlagToggleRow(
                    title = "Floor Control V2",
                    description = "Enable queue/lease-based floor arbitration.",
                    checked = flags.floorV2,
                    onCheckedChange = featureFlagsRepository::updateFloorV2
                )
                FlagToggleRow(
                    title = "Observability V2",
                    description = "Enable additional diagnostics and metrics.",
                    checked = flags.observabilityV2,
                    onCheckedChange = featureFlagsRepository::updateObservabilityV2
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
                    text = "Diagnostics",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Logs folder: ${deviceLogStore.logsDirectoryPath()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shareLogFile(
                                context = context,
                                file = deviceLogStore.appLogFile(),
                                chooserTitle = "Share app log"
                            )
                        }
                    ) {
                        Text("Share App Log")
                    }
                    Button(
                        onClick = {
                            shareLogFile(
                                context = context,
                                file = deviceLogStore.crashLogFile(),
                                chooserTitle = "Share crash log"
                            )
                        }
                    ) {
                        Text("Share Crash")
                    }
                    Button(
                        onClick = {
                            deviceLogStore.clearLogs()
                            Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Clear Logs")
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
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
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
    ThemeOptionUi(ThemeColor.ORANGE, Color(0xFFFF8A00), Color(0xFFFFB347)),
    ThemeOptionUi(ThemeColor.RED, Color(0xFFFF5252), Color(0xFFFF8A80)),
    ThemeOptionUi(ThemeColor.BLUE, Color(0xFF4DA3FF), Color(0xFF82C4FF)),
    ThemeOptionUi(ThemeColor.GREEN, Color(0xFF4CD964), Color(0xFF85F39B)),
    ThemeOptionUi(ThemeColor.YELLOW, Color(0xFFFFD54F), Color(0xFFFFE082))
)

private fun themeOptionFor(themeColor: ThemeColor): ThemeOptionUi {
    return themeOptions.firstOrNull { it.themeColor == themeColor }
        ?: themeOptions.first()
}
