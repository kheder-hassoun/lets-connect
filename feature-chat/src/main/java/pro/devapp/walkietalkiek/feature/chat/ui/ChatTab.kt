package pro.devapp.walkietalkiek.feature.chat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.feature.chat.ChatViewModel
import pro.devapp.walkietalkiek.feature.chat.model.ChatAction

@Composable
fun ChatTab() {

    val viewModel = koinInject<ChatViewModel>()
    val state = viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(ChatAction.InitScreen)
        viewModel.startCollectingConnectedDevices()
    }

    ChatContent(
        messages = state.value.messages,
        currentUserId = "me",
        onSendMessage = { message ->
            viewModel.onAction(ChatAction.SendMessage(message))
        }
    )
}
