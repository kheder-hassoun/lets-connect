package pro.devapp.walkietalkiek.feature.ptt

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.core.mvi.MviViewModel
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState
import pro.devapp.walkietalkiek.serivce.network.FloorControlProtocol
import pro.devapp.walkietalkiek.serivce.network.FloorPublisher
import pro.devapp.walkietalkiek.serivce.network.MessageController
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
import pro.devapp.walkietalkiek.service.voice.VoicePlayer

internal class PttViewModel(
    actionProcessor: PttActionProcessor,
    private val connectedDevicesRepository: ConnectedDevicesRepository,
    private val messageController: MessageController,
    private val floorPublisher: FloorPublisher,
    private val pttFloorRepository: PttFloorRepository,
    private val voicePlayer: VoicePlayer,
    private val appSettingsRepository: AppSettingsRepository
): MviViewModel<PttScreenState, PttAction, PttEvent>(
    actionProcessor = actionProcessor
) {
    private var talkTimerJob: Job? = null
    private var collectorsStarted = false

    fun startCollectingConnectedDevices() {
        if (collectorsStarted) return
        collectorsStarted = true
        viewModelScope.launch {
            var lastConnectedHosts = emptySet<String>()
            connectedDevicesRepository.clientsFlow.collect {
                val connectedHosts = it.asSequence()
                    .filter { client -> client.isConnected }
                    .map { client -> client.hostAddress }
                    .toSet()
                val newConnectedHosts = connectedHosts - lastConnectedHosts
                if (newConnectedHosts.isNotEmpty() && state.value.isRecording) {
                    // Event-driven re-announce: a peer connected while I hold PTT,
                    // so send floor taken once more to synchronize lock state.
                    val published = floorPublisher.publishAcquire()
                    if (!published) {
                        messageController.sendMessage(FloorControlProtocol.acquirePacket())
                    }
                }
                lastConnectedHosts = connectedHosts
                onPttAction(PttAction.ConnectedDevicesUpdated(it))
            }
        }
        viewModelScope.launch {
            var lastWaveUpdateAt = 0L
            voicePlayer.voiceDataFlow.collect {
                val now = System.currentTimeMillis()
                if (now - lastWaveUpdateAt >= WAVE_UI_UPDATE_THROTTLE_MS) {
                    onPttAction(PttAction.VoiceDataReceived(it))
                    lastWaveUpdateAt = now
                }
            }
        }
        viewModelScope.launch {
            appSettingsRepository.settings.collect {
                onPttAction(PttAction.TalkDurationChanged(it.talkDurationSeconds))
            }
        }
        viewModelScope.launch {
            pttFloorRepository.currentFloorOwnerHost.collect {
                onPttAction(PttAction.FloorOwnerChanged(it))
            }
        }
    }

    fun onPttAction(action: PttAction) {
        when (action) {
            PttAction.StartRecording -> {
                val currentState = state.value
                val isBlockedByRemote = currentState.floorOwnerHostAddress != null &&
                    !currentState.isFloorHeldByMe
                if (!currentState.isRecording && !isBlockedByRemote) {
                    startTalkTimer()
                }
            }
            PttAction.StopRecording -> {
                if (state.value.isRecording) {
                    stopTalkTimer()
                }
            }
            else -> Unit
        }
        onAction(action)
    }

    private fun startTalkTimer() {
        stopTalkTimer()
        talkTimerJob = viewModelScope.launch {
            val totalMillis = appSettingsRepository.settings.value.talkDurationSeconds * 1000L
            val tickMillis = 100L
            var remainMillis = totalMillis
            onPttAction(PttAction.TalkTimerTick(remainMillis))
            while (remainMillis > 0) {
                delay(tickMillis)
                remainMillis = (remainMillis - tickMillis).coerceAtLeast(0L)
                onPttAction(PttAction.TalkTimerTick(remainMillis))
            }
            onPttAction(PttAction.StopRecording)
        }
    }

    private fun stopTalkTimer() {
        talkTimerJob?.cancel()
        talkTimerJob = null
    }
}

private const val WAVE_UI_UPDATE_THROTTLE_MS = 70L
