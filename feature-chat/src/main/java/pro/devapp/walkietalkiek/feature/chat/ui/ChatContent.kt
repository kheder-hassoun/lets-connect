package pro.devapp.walkietalkiek.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.feature.chat.model.ChatMessageModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatContent(
    modifier: Modifier = Modifier,
    messages: List<ChatMessageModel> = emptyList(),
    currentUserId: String = "current_user", // This should come from user session
    onSendMessage: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when messages are updated
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No messages yet.\nStart the conversation.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB8B8B8),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 10.dp,
                top = 10.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                MessageItem(
                    message = message,
                    isOutgoing = message.sender == currentUserId
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            InputSection(
                onSendMessage = { message ->
                    if (message.isNotBlank()) {
                        onSendMessage(message)
                    }
                }
            )
        }
    }
}
