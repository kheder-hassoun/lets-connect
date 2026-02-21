package pro.devapp.walkietalkiek.service.voice

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
import pro.devapp.walkietalkiek.serivce.network.MessageController
import timber.log.Timber

class VoiceRecorder(
    private val chanelController: MessageController,
    private val coroutineContextProvider: CoroutineContextProvider,
    private val pttTonePlayer: PttTonePlayer
) {
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineScope = coroutineContextProvider.createScope(
        coroutineContextProvider.io.limitedParallelism(1)
    )

    private var audioRecord: AudioRecord? = null
    private var readBufferSize = DEFAULT_FRAME_BYTES

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun create() {
        Timber.Forest.i("create")
        pttTonePlayer.init()
        val sampleRate = getSupportedSampleRate()
        sampleRate?.let  {
            Timber.Forest.i("sampleRate: $it")
            readBufferSize = ((it * BYTES_PER_SAMPLE * FRAME_DURATION_MS) / 1000)
                .coerceAtLeast(MIN_FRAME_BYTES)
            var bufferSize = readBufferSize * AUDIO_RECORD_FRAMES_IN_BUFFER
            val minBufferSize = getMinBufferSize(sampleRate)
            if (bufferSize < minBufferSize) bufferSize = minBufferSize
            Timber.Forest.i("internal audio buffer size: $bufferSize")
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        }
    }

    fun destroy() {
        Timber.Forest.i("destroy")
        coroutineScope.cancel()
        audioRecord?.apply {
            release()
        }
        pttTonePlayer.release()
    }

    fun startRecord() {
        Timber.Forest.i("startRecord")
        pttTonePlayer.play()
        audioRecord?.apply {
            startRecording()
        }
        startReading()
    }

    fun stopRecord() {
        Timber.Forest.i("stopRecord")
        audioRecord?.apply { stop() }
    }

    private fun startReading() {
        Timber.Forest.i("startReading")
        coroutineScope.launch {
            // Give FLOOR_TAKEN control event a head-start before any voice packet is sent.
            delay(PRE_TRANSMIT_GUARD_MS)
            val rawFrame = ByteArray(readBufferSize)
            val outgoingFrame = ByteArray(readBufferSize + 1)
            outgoingFrame[0] = AUDIO_PACKET_PREFIX
            while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.apply {
                    val readCount = read(rawFrame, 0, readBufferSize, AudioRecord.READ_BLOCKING)
                    if (readCount > 0) {
                        val payload = if (readCount == readBufferSize) {
                            System.arraycopy(rawFrame, 0, outgoingFrame, 1, readCount)
                            outgoingFrame
                        } else {
                            ByteArray(readCount + 1).also {
                                it[0] = AUDIO_PACKET_PREFIX
                                System.arraycopy(rawFrame, 0, it, 1, readCount)
                            }
                        }
                        chanelController.sendMessage(payload)
                    }
                }
            }
        }
    }

    private fun getSupportedSampleRate(): Int? {
        val rates = arrayOf(16000, 22050, 44100, 11025, 8000)
        rates.forEach {
            val minBufferSize = getMinBufferSize(it)
            if (minBufferSize != AudioRecord.ERROR &&
                minBufferSize != AudioRecord.ERROR_BAD_VALUE
            ) {
                return it
            }
        }
        return null
    }

    private fun getMinBufferSize(rate: Int): Int {
        return AudioRecord.getMinBufferSize(
            rate, channelConfig, AudioFormat.ENCODING_PCM_16BIT
        )
    }
}

private const val AUDIO_PACKET_PREFIX: Byte = 1
private const val BYTES_PER_SAMPLE = 2
private const val FRAME_DURATION_MS = 20
private const val AUDIO_RECORD_FRAMES_IN_BUFFER = 4
private const val MIN_FRAME_BYTES = 256
private const val DEFAULT_FRAME_BYTES = 640
private const val PRE_TRANSMIT_GUARD_MS = 180L
