package pro.devapp.walkietalkiek.ui

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.TextMessagesRepository
import pro.devapp.walkietalkiek.service.WalkieService
import kotlin.system.exitProcess

@Composable
internal fun OffContent() {
    val activity = LocalActivity.current
    val appSettingsRepository = koinInject<AppSettingsRepository>()
    val connectedDevicesRepository = koinInject<ConnectedDevicesRepository>()
    val textMessagesRepository = koinInject<TextMessagesRepository>()
    var statusText by remember { mutableStateOf("Use these tools to recover app state.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    text = "Fix & Recovery",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                connectedDevicesRepository.clearAll()
                textMessagesRepository.clearAll()
                appSettingsRepository.resetToDefaults()
                statusText = "Cleared runtime data and restored default settings."
            }
        ) {
            Text("Clear Data + Reset Settings")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val serviceIntent = Intent(activity, WalkieService::class.java)
                activity?.stopService(serviceIntent)
                activity?.startService(serviceIntent)
                statusText = "Walkie service restarted."
            }
        ) {
            Text("Restart Walkie Service")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val launchIntent = activity?.packageManager?.getLaunchIntentForPackage(activity.packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                if (launchIntent != null) {
                    activity?.startActivity(launchIntent)
                    activity?.finish()
                }
            }
        ) {
            Text("Restart App")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val serviceIntent = Intent(activity, WalkieService::class.java)
                activity?.stopService(serviceIntent)
                activity?.finishAndRemoveTask()
                exitProcess(0)
            }
        ) {
            Text("Force Close App")
        }
    }
}
