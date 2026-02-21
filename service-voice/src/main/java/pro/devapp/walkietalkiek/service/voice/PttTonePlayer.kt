package pro.devapp.walkietalkiek.service.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import timber.log.Timber

class PttTonePlayer(
    private val context: Context,
    private val appSettingsRepository: AppSettingsRepository
) {
    private var soundPool: SoundPool? = null
    private var toneSoundId: Int? = null
    private var isLoaded = false
    private var toneResId = 0

    fun init() {
        if (soundPool != null) {
            return
        }
        release()
        toneResId = resolveToneResId()
        if (toneResId == 0) {
            Timber.Forest.w("PTT tone file not found. Expected raw resource: ptt_tone")
            return
        }

        runCatching {
            soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
                .apply {
                    setOnLoadCompleteListener { _, _, status ->
                        isLoaded = status == 0
                        if (!isLoaded) {
                            Timber.Forest.w("PTT tone failed to load, status=$status")
                        }
                    }
                }
            toneSoundId = soundPool?.load(context, toneResId, 1)
        }.onFailure { error ->
            Timber.Forest.w(error, "Failed to initialize PTT tone player")
            release()
        }
    }

    fun play() {
        if (!appSettingsRepository.settings.value.toneEnabled) {
            return
        }
        if (toneResId == 0 || soundPool == null) {
            init()
        }
        if (!isLoaded) {
            return
        }
        toneSoundId?.let { soundId ->
            runCatching {
                soundPool?.play(
                    soundId,
                    1f,
                    1f,
                    1,
                    0,
                    1f
                )
            }.onFailure { error ->
                Timber.Forest.w(error, "PTT tone play failed")
            }
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        toneSoundId = null
        isLoaded = false
    }

    private fun resolveToneResId(): Int {
        return context.resources.getIdentifier(
            "ptt_tone",
            "raw",
            context.packageName
        )
    }
}
