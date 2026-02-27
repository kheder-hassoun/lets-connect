package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel

@Composable
internal fun ConnectedPeersList(
    devices: List<ClientModel>,
    modifier: Modifier = Modifier
) {
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val base = screenWidth.coerceAtMost(screenHeight)
    val scale = (base / 400.dp).coerceIn(0.82f, 1.2f)
    val connectedCount = devices.count { it.isConnected }
    val shape = RoundedCornerShape((14 * scale).dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = (14 * scale).dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    shape = shape
                )
                .padding(horizontal = (12 * scale).dp, vertical = (10 * scale).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Peers Available",
                    fontSize = (12 * scale).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = connectedCount.toString(),
                fontSize = (26 * scale).sp,
                color = if (connectedCount > 0) Color(0xFF3BD98A) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (connectedCount > 0) "Ready to receive" else "No active peers",
                fontSize = (11 * scale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
