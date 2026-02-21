package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun DeviceItem(
    modifier: Modifier = Modifier,
    isOnline: Boolean,
    address: String
) {
    val color = if (isOnline) {
        Color(0xFF3BD98A)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isOnline) {
                            listOf(
                                Color(0x163BD98A),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isOnline) Color(0x403BD98A) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(10.dp)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
            )
            Text(
                modifier = Modifier.fillMaxWidth(0.78f),
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
