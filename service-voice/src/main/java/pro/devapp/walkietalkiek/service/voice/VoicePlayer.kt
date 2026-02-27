package pro.devapp.walkietalkiek.service.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import pro.devapp.walkietalkiek.serivce.network.SocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.serivce.network.SocketServer
import timber.log.Timber

class VoicePlayer(
    private val socketClient: SocketClient,
    private val socketServer: SocketServer,
    private val pttTonePlayer: PttTonePlayer
) {
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val playbackLock = Any()
    private var audioTrack: AudioTrack? = null
    private var bufferSize = 0
    private var lastVoicePacketAt = 0L
    private val remoteSessionGapMs = 650L
    private val remoteBurstCheckIntervalMs = 150L
    private var remoteBurstActive = false
    private var remoteBurstMonitorJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _voiceDataFlow = MutableSharedFlow<ByteArray>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val voiceDataFlow: SharedFlow<ByteArray>
        get() = _voiceDataFlow

    fun create() {
        runCatching {
            pttTonePlayer.init()
            val sampleRate = getSupportedSampleRate()
            if (sampleRate == null) {
                Timber.Forest.w("No supported sample rate for VoicePlayer.")
                return@runCatching
            }
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val frameBytes = ((sampleRate * BYTES_PER_SAMPLE * FRAME_DURATION_MS) / 1000)
                .coerceAtLeast(MIN_FRAME_BYTES)
            bufferSize = frameBytes * AUDIO_TRACK_FRAMES_IN_BUFFER
            if (bufferSize < minBufferSize) bufferSize = minBufferSize
            val playbackFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build()
            val builder = AudioTrack.Builder()
                .setAudioAttributes(playbackAttributes)
                .setAudioFormat(playbackFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
            audioTrack = builder.build()
            audioTrack?.apply {
                play()
            }
        }.onFailure { error ->
            Timber.Forest.w(error, "VoicePlayer create failed")
            runCatching { audioTrack?.release() }
            audioTrack = null
        }
        val onVoicePacket: (ByteArray) -> Unit = { bytes ->
            synchronized(playbackLock) {
                play(bytes)
            }
        }
        socketServer.dataListener = onVoicePacket
        socketClient.dataListener = onVoicePacket
        if (remoteBurstMonitorJob?.isActive != true) {
            remoteBurstMonitorJob = scope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    if (remoteBurstActive && now - lastVoicePacketAt > remoteSessionGapMs) {
                        remoteBurstActive = false
                        runCatching {
                            pttTonePlayer.playRelease()
                        }.onFailure { error ->
                            Timber.Forest.w(error, "PTT release tone play failed on remote burst end")
                        }
                    }
                    delay(remoteBurstCheckIntervalMs)
                }
            }
        }
    }

    private fun play(bytes: ByteArray) {
        if (bytes.isEmpty()) {
            return
        }
        val pcmBytes = if (bytes.size and 1 == 1) {
            bytes.copyOf(bytes.size - 1)
        } else {
            bytes
        }
        if (pcmBytes.isEmpty()) {
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastVoicePacketAt > remoteSessionGapMs) {
            // Play an attention tone once at the start of each remote speaking burst.
            runCatching {
                pttTonePlayer.play()
            }.onFailure { error ->
                Timber.Forest.w(error, "PTT receive tone play failed")
            }
        }
        lastVoicePacketAt = now
        remoteBurstActive = true

        runCatching {
            val track = audioTrack ?: return@runCatching
            if (track.state != AudioTrack.STATE_INITIALIZED) {
                Timber.Forest.w("AudioTrack is not initialized; dropping incoming frame")
                return@runCatching
            }
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
            val written = track.write(pcmBytes, 0, pcmBytes.size, AudioTrack.WRITE_NON_BLOCKING)
            if (written < 0) {
                Timber.Forest.w("AudioTrack write failed code=$written, frame=${pcmBytes.size}")
            }
        }.onFailure { error ->
            Timber.Forest.w(error, "Voice playback failed; dropping frame")
        }
        _voiceDataFlow.tryEmit(pcmBytes)
    }

    fun shutdown() {
        synchronized(playbackLock) {
            socketServer.dataListener = null
            socketClient.dataListener = null
            remoteBurstMonitorJob?.cancel()
            remoteBurstMonitorJob = null
            remoteBurstActive = false
            audioTrack?.apply {
                runCatching { stop() }
                runCatching { release() }
            }
            audioTrack = null
            pttTonePlayer.release()
            scope.cancel()
        }
    }


    private fun getSupportedSampleRate(): Int? {
        val rates = arrayOf(16000, 22050, 44100, 11025, 8000)
        rates.forEach {
            val minBufferSize = AudioTrack.getMinBufferSize(
                it, channelConfig, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufferSize != AudioTrack.ERROR &&
                minBufferSize != AudioTrack.ERROR_BAD_VALUE
            ) {
                return it
            }
        }
        return null
    }
}

private const val BYTES_PER_SAMPLE = 2
private const val FRAME_DURATION_MS = 20
private const val AUDIO_TRACK_FRAMES_IN_BUFFER = 8
private const val MIN_FRAME_BYTES = 256
