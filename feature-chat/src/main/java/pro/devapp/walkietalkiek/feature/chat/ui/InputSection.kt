package pro.devapp.walkietalkiek.feature.chat.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
internal fun InputSection(
    onSendMessage: (String) -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    val sendMessage: () -> Unit = {
        if (messageText.isNotBlank()) {
            onSendMessage(messageText.trim())
            messageText = ""
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action == AndroidKeyEvent.ACTION_UP &&
                            keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                        ) {
                            sendMessage()
                            true
                        } else {
                            false
                        }
                    },
                placeholder = {
                    Text(
                        text = "Message...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                },
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { sendMessage() },
                    onDone = { sendMessage() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            IconButton(
                onClick = { sendMessage() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(
                        color = if (messageText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (messageText.isNotBlank()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
            }
        }
    }
}
