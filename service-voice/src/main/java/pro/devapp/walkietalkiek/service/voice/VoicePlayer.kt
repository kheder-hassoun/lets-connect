package pro.devapp.walkietalkiek.service.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import pro.devapp.walkietalkiek.serivce.network.SocketClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import pro.devapp.walkietalkiek.serivce.network.SocketServer
import timber.log.Timber

class VoicePlayer(
    private val socketClient: SocketClient,
    private val socketServer: SocketServer,
    private val pttTonePlayer: PttTonePlayer
) {
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private var audioTrack: AudioTrack? = null
    private var bufferSize = 0
    private var lastVoicePacketAt = 0L
    private val remoteSessionGapMs = 1200L

    private val _voiceDataFlow = MutableSharedFlow<ByteArray>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val voiceDataFlow: SharedFlow<ByteArray>
        get() = _voiceDataFlow

    fun create() {
        pttTonePlayer.init()
        val sampleRate = getSupportedSampleRate()
        sampleRate?.let {
            val minBufferSize = AudioTrack.getMinBufferSize(
                it,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val frameBytes = ((it * BYTES_PER_SAMPLE * FRAME_DURATION_MS) / 1000)
                .coerceAtLeast(MIN_FRAME_BYTES)
            bufferSize = frameBytes * AUDIO_TRACK_FRAMES_IN_BUFFER
            if (bufferSize < minBufferSize) bufferSize = minBufferSize
            val playbackFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(it)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
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
        }
        val onVoicePacket: (ByteArray) -> Unit = { bytes ->
            play(bytes)
        }
        socketServer.dataListener = onVoicePacket
        socketClient.dataListener = onVoicePacket
    }

    private fun play(bytes: ByteArray) {
        val now = System.currentTimeMillis()
        if (now - lastVoicePacketAt > remoteSessionGapMs) {
            // Play an attention tone once at the start of each remote speaking burst.
            pttTonePlayer.play()
        }
        lastVoicePacketAt = now

        if (audioTrack?.playState == AudioTrack.PLAYSTATE_STOPPED) {
            Timber.Forest.w("PLAYER STOPPED!!!")
        }
        audioTrack?.write(bytes, 0, bytes.size, AudioTrack.WRITE_NON_BLOCKING)
        _voiceDataFlow.tryEmit(bytes)
    }

    fun shutdown() {
        socketServer.dataListener = null
        socketClient.dataListener = null
        audioTrack?.apply {
            stop()
            release()
        }
        pttTonePlayer.release()
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
