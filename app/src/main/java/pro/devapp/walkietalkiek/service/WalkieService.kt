package pro.devapp.walkietalkiek.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import pro.devapp.walkietalkiek.serivce.network.ClientController
import pro.devapp.walkietalkiek.service.voice.VoicePlayer
import pro.devapp.walkietalkiek.service.voice.VoiceRecorder
import timber.log.Timber

class WalkieService: Service() {

    private val chanelController: ClientController by inject()
    private val notificationController: NotificationController by inject()
    private val voiceRecorder: VoiceRecorder by inject()
    private val voicePlayer: VoicePlayer by inject()

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var runtimeInitialized = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("WalkieService onCreate")
        runCatching { setWakeLock() }
            .onFailure { error -> Timber.Forest.w(error, "Wake lock init failed") }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("WalkieService onStartCommand startId=%d flags=%d", startId, flags)
        notificationController.createNotificationChanel()
        ServiceCompat.startForeground(
            this,
            NotificationController.NOTIFICATION_ID,
            notificationController.createNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )
        initializeRuntimeIfNeeded()
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        Timber.i("WalkieService onDestroy")
        super.onDestroy()
        serviceScope.cancel()
        runCatching { voiceRecorder.stopRecord() }
        runCatching { voiceRecorder.destroy() }
        runCatching { voicePlayer.shutdown() }
        runCatching { chanelController.stopDiscovery() }
        runCatching { releaseWakeLock() }
    }

    private fun setWakeLock() {
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).run {
                newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "WalkieTalkyApp::ServiceWakelockTag"
                ).apply {
                    acquire(10*60*1000L /*10 minutes*/)
                }
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
    }

    private fun initializeRuntimeIfNeeded() {
        if (runtimeInitialized) {
            Timber.i("WalkieService runtime already initialized")
            return
        }
        Timber.i("WalkieService initializeRuntimeIfNeeded start")
        runtimeInitialized = true
        serviceScope.launch {
            runCatching {
                voicePlayer.create()
                chanelController.startDiscovery()
                Timber.i("WalkieService runtime initialized successfully")
            }.onFailure { error ->
                Timber.Forest.w(error, "Service runtime init failed")
                runtimeInitialized = false
                stopSelf()
            }
        }
    }
}
