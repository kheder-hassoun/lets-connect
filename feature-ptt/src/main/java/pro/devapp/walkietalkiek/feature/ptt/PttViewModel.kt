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
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.service.voice.VoicePlayer

internal class PttViewModel(
    actionProcessor: PttActionProcessor,
    private val connectedDevicesRepository: ConnectedDevicesRepository,
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
            connectedDevicesRepository.clientsFlow.collect {
                onPttAction(PttAction.ConnectedDevicesUpdated(it))
            }
        }
        viewModelScope.launch {
            voicePlayer.voiceDataFlow.collect {
                onPttAction(PttAction.VoiceDataReceived(it))
            }
        }
        viewModelScope.launch {
            appSettingsRepository.settings.collect {
                onPttAction(PttAction.TalkDurationChanged(it.talkDurationSeconds))
            }
        }
    }

    fun onPttAction(action: PttAction) {
        when (action) {
            PttAction.StartRecording -> startTalkTimer()
            PttAction.StopRecording -> stopTalkTimer()
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
