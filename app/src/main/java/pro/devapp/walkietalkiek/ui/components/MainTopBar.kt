package pro.devapp.walkietalkiek.ui.components

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import pro.devapp.walkietalkiek.model.MainScreenState

@Composable
fun MainTopBar(
    modifier: Modifier = Modifier,
    state: MainScreenState
) {
    val accent = MaterialTheme.colorScheme.primary
    val accentSoft = MaterialTheme.colorScheme.secondary
    val isReadyToTalk = state.requiredPermissions.isEmpty()
    val liveColor = if (isReadyToTalk) Color(0xFF48E37B) else Color(0xFFFF7043)
    val liveLabel = if (isReadyToTalk) "Ready to talk" else "Permissions needed"
    val currentTabTitle = state.mainTabs.firstOrNull { it.screen == state.currentTab }?.title ?: state.currentTab.name
    val pulseTransition = rememberInfiniteTransition(label = "live-pointer")
    val liveDotAlpha = pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live-dot-alpha"
    ).value
    Surface(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 10.dp,
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.4f),
                            accentSoft.copy(alpha = 0.26f),
                            Color(0x1F0D0D0D)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                setImageDrawable(ctx.packageManager.getApplicationIcon(ctx.packageName))
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 1.dp)
                    )
                    Text(
                        text = "Let's-Connect",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                color = liveColor.copy(alpha = liveDotAlpha),
                                shape = RoundedCornerShape(999.dp)
                            )
                    )
                    Text(
                        text = liveLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = 0.18f)
            ) {
                Text(
                    text = currentTabTitle,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = accentSoft,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
