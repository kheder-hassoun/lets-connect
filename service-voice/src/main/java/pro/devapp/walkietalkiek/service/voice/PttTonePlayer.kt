package pro.devapp.walkietalkiek.service.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.core.settings.PttToneProfile
import timber.log.Timber

class PttTonePlayer(
    private val context: Context,
    private val appSettingsRepository: AppSettingsRepository
) {
    private var soundPool: SoundPool? = null
    private var toneSoundId: Int? = null
    private var isLoaded = false
    private var toneResId = 0
    private var activeToneProfile: PttToneProfile? = null

    fun init() {
        val profile = appSettingsRepository.settings.value.toneProfile
        if (soundPool != null && profile == activeToneProfile) {
            return
        }
        release()
        toneResId = resolveToneResId(profile)
        if (toneResId == 0) {
            Timber.Forest.w("PTT tone file not found. Expected raw resource: ptt_tone")
            return
        }
        activeToneProfile = profile

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
    }

    fun play() {
        val profile = appSettingsRepository.settings.value.toneProfile
        if (profile != activeToneProfile || toneResId == 0 || soundPool == null) {
            init()
        }
        if (!isLoaded) {
            return
        }
        toneSoundId?.let { soundId ->
            soundPool?.play(
                soundId,
                1f,
                1f,
                1,
                0,
                1f
            )
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

    private fun resolveToneResId(profile: PttToneProfile): Int {
        val name = when (profile) {
            PttToneProfile.CLASSIC -> "ptt_tone"
            PttToneProfile.SOFT -> "ptt_tone_soft"
            PttToneProfile.SHARP -> "ptt_tone_sharp"
        }
        val requested = context.resources.getIdentifier(name, "raw", context.packageName)
        if (requested != 0) {
            return requested
        }
        return resolveToneResId()
    }
}
