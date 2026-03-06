package pro.devapp.walkietalkiek.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pro.devapp.walkietalkiek.R
import androidx.compose.ui.res.stringResource

@Composable
internal fun LowCallVolumeOverlay(
    currentPercent: Int,
    onOkClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x66B91C1C),
                        Color(0x55A11A1A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B0A0A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.call_volume_warning_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFFFCA5A5)
                )
                Text(
                    text = stringResource(
                        R.string.call_volume_warning_body,
                        currentPercent,
                        CALL_VOLUME_MIN_PERCENT
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFEE2E2)
                )
                Button(
                    onClick = onOkClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.call_volume_warning_ok),
                        color = Color.White
                    )
                }
            }
        }
    }
}

internal const val CALL_VOLUME_MIN_PERCENT = 40
