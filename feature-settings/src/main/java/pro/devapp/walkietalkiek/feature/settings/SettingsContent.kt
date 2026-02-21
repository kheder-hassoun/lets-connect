package pro.devapp.walkietalkiek.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.core.settings.ThemeColor
import kotlin.math.roundToInt

@Immutable
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

@Composable
fun SettingsContent() {
    val settingsRepository = koinInject<AppSettingsRepository>()
    val settings by settingsRepository.settings.collectAsState()
    val contentPadding = 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
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
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth < 420.dp) 2 else 3
                    val spacing = if (columns == 2) 12.dp else 10.dp
                    val rows = themeOptions.chunked(columns)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        rows.forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                rowOptions.forEach { option ->
                                    ThemeColorTile(
                                        modifier = Modifier.weight(1f),
                                        option = option,
                                        selected = settings.themeColor == option.themeColor,
                                        onClick = {
                                            settingsRepository.updateThemeColor(option.themeColor)
                                        }
                                    )
                                }
                                repeat(columns - rowOptions.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeColorTile(
    modifier: Modifier = Modifier,
    option: ThemeOptionUi,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) option.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val selectionLabel = if (selected) "Selected" else option.themeColor.title
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        option.primary.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.45f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(option.primary, option.accent)
                    )
                ),
            contentAlignment = Alignment.BottomEnd
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
        Text(
            text = selectionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
