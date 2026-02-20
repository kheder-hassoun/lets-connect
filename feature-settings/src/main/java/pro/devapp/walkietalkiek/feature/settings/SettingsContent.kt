package pro.devapp.walkietalkiek.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.core.settings.PttToneProfile
import kotlin.math.roundToInt

@Composable
fun SettingsContent() {
    val settingsRepository = koinInject<AppSettingsRepository>()
    val settings by settingsRepository.settings.collectAsState()

    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = androidx.compose.ui.Modifier
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
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PTT Tone",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PttToneProfile.entries.forEach { profile ->
                        FilterChip(
                            selected = settings.toneProfile == profile,
                            onClick = {
                                settingsRepository.updateToneProfile(profile)
                            },
                            label = { Text(profile.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Text(
                    text = "Tone files: ptt_tone / ptt_tone_soft / ptt_tone_sharp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
