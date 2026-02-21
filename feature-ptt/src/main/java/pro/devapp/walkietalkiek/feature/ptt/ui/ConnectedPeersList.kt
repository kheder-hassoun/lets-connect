package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel

@Composable
internal fun ConnectedPeersList(
    devices: List<ClientModel>,
    modifier: Modifier = Modifier
) {
    val sortedDevices = devices.sortedWith(
        compareByDescending<ClientModel> { it.isConnected }
            .thenBy { it.hostAddress }
    )
    val onlineCount = sortedDevices.count { it.isConnected }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Peers ($onlineCount/${sortedDevices.size})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        if (sortedDevices.isEmpty()) {
            Text(
                text = "No peers discovered yet. Keep both devices on the same LAN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                items = sortedDevices,
                key = { it.hostAddress }
            ) { device ->
                DeviceItem(
                    isOnline = device.isConnected,
                    address = "${device.hostAddress}:${device.port}"
                )
            }
        }
    }
}
