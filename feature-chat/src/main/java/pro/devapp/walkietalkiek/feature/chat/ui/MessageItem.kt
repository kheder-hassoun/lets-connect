package pro.devapp.walkietalkiek.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.devapp.walkietalkiek.feature.chat.model.ChatMessageModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun MessageItem(
    message: ChatMessageModel,
    isOutgoing: Boolean
) {
    val alignment: Alignment.Horizontal = if (isOutgoing) Alignment.End else Alignment.Start
    val backgroundColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val senderColor = MaterialTheme.colorScheme.secondary

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val bubbleMaxWidth = (maxWidth * 0.78f).coerceAtLeast(220.dp)
        val sideInset: Dp = (maxWidth * 0.14f).coerceIn(20.dp, 72.dp)
        val contentInset = (bubbleMaxWidth * 0.045f).coerceIn(10.dp, 14.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = alignment
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = bubbleMaxWidth)
                    .padding(
                        start = if (isOutgoing) sideInset else 0.dp,
                        end = if (isOutgoing) 0.dp else sideInset
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOutgoing) 16.dp else 4.dp,
                    bottomEnd = if (isOutgoing) 4.dp else 16.dp
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(contentInset)
                ) {
                    if (!isOutgoing) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = message.sender,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = senderColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )

                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (message.isRead) "✓✓" else "✓",
                                fontSize = 11.sp,
                                color = if (message.isRead) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    textColor.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "now" // Less than 1 minute
        diff < 3600_000 -> "${diff / 60_000}m" // Less than 1 hour
        diff < 86400_000 -> { // Less than 24 hours
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        else -> { // More than 24 hours
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
