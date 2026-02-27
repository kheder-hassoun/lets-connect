package pro.devapp.walkietalkiek.feature.ptt

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.core.flags.FeatureFlagsRepository
import pro.devapp.walkietalkiek.core.mvi.MviViewModel
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState
import pro.devapp.walkietalkiek.serivce.network.FloorControlProtocol
import pro.devapp.walkietalkiek.serivce.network.MessageController
import pro.devapp.walkietalkiek.serivce.network.data.ClusterMembershipRepository
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
import pro.devapp.walkietalkiek.service.voice.VoicePlayer

internal class PttViewModel(
    actionProcessor: PttActionProcessor,
    private val connectedDevicesRepository: ConnectedDevicesRepository,
    private val clusterMembershipRepository: ClusterMembershipRepository,
    private val featureFlagsRepository: FeatureFlagsRepository,
    private val messageController: MessageController,
    private val pttFloorRepository: PttFloorRepository,
    private val voicePlayer: VoicePlayer,
    private val appSettingsRepository: AppSettingsRepository
): MviViewModel<PttScreenState, PttAction, PttEvent>(
    actionProcessor = actionProcessor
) {
    private var talkTimerJob: Job? = null
    private var floorRequestTimeoutJob: Job? = null
    private var floorRequestRetryJob: Job? = null
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
                    if (!featureFlagsRepository.flags.value.serverlessControl) {
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
        viewModelScope.launch {
            clusterMembershipRepository.status.collect { status ->
                val leaderLabel = status.leaderNodeId.ifBlank { "--" }
                val role = when (status.role) {
                    pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER -> "Leader"
                    pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.PEER -> "Peer"
                }
                onPttAction(
                    PttAction.ClusterStatusChanged(
                        selfNodeId = status.selfNodeId,
                        roleLabel = role,
                        leaderNodeLabel = leaderLabel,
                        membersCount = status.activeMembersCount
                    )
                )
            }
        }
        viewModelScope.launch {
            state.collect { current ->
                if (current.isFloorRequestPending && !current.isRecording) {
                    if (floorRequestTimeoutJob?.isActive != true) {
                        floorRequestTimeoutJob = viewModelScope.launch {
                            delay(FLOOR_REQUEST_TIMEOUT_MS)
                            if (state.value.isFloorRequestPending && !state.value.isRecording) {
                                onPttAction(PttAction.StopRecording)
                            }
                        }
                    }
                    if (floorRequestRetryJob?.isActive != true) {
                        floorRequestRetryJob = viewModelScope.launch {
                            delay(FLOOR_REQUEST_RETRY_INITIAL_DELAY_MS)
                            while (state.value.isFloorRequestPending && !state.value.isRecording) {
                                onPttAction(PttAction.StartRecording)
                                delay(FLOOR_REQUEST_RETRY_MS)
                            }
                        }
                    }
                } else {
                    floorRequestTimeoutJob?.cancel()
                    floorRequestTimeoutJob = null
                    floorRequestRetryJob?.cancel()
                    floorRequestRetryJob = null
                }
            }
        }
    }

    fun onPttAction(action: PttAction) {
        when (action) {
            PttAction.StartRecording -> {
                val currentState = state.value
                val isBlockedByRemote = currentState.floorOwnerHostAddress != null &&
                    !currentState.isFloorHeldByMe
                val isServerlessLeasePath = featureFlagsRepository.flags.value.serverlessControl
                if (!isServerlessLeasePath && !currentState.isRecording && !isBlockedByRemote) {
                    startTalkTimer()
                }
            }
            PttAction.StartRecordingGranted -> {
                val currentState = state.value
                if (!currentState.isRecording) {
                    startTalkTimer()
                }
            }
            PttAction.StopRecording -> {
                if (state.value.isRecording) {
                    stopTalkTimer()
                }
                floorRequestTimeoutJob?.cancel()
                floorRequestTimeoutJob = null
                floorRequestRetryJob?.cancel()
                floorRequestRetryJob = null
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

private const val WAVE_UI_UPDATE_THROTTLE_MS = 140L
private const val FLOOR_REQUEST_TIMEOUT_MS = 7000L
private const val FLOOR_REQUEST_RETRY_INITIAL_DELAY_MS = 500L
private const val FLOOR_REQUEST_RETRY_MS = 900L
