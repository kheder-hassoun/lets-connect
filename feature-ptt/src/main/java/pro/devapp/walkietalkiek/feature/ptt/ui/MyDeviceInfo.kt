package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

@Composable
fun MyDeviceInfo(
    modifier: Modifier = Modifier,
    isOnline: Boolean,
    addressIp: String
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val scale = (screenWidth.coerceAtMost(screenHeight) / 400.dp).coerceIn(0.82f, 1.2f)
    val color = if (isOnline) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.5f)
    }
    Row(
        modifier = modifier.padding(horizontal = (16 * scale).dp, vertical = (10 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = (8 * scale).dp)
                .size((12 * scale).dp)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )
        Text(
            text = addressIp,
            fontSize = (13 * scale).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
