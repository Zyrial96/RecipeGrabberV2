package com.recipegrabber.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.recipegrabber.R
import com.recipegrabber.data.logging.AppLogger
import com.recipegrabber.data.repository.PreferencesRepository
import com.recipegrabber.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ClipboardMonitorService : Service() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var logger: AppLogger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var clipboardManager: ClipboardManager
    private var clipboardListenerRegistered = false
    private var settingsObserverStarted = false

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        onClipboardChanged()
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.i("Clipboard", "Service started")
        startForeground(NOTIFICATION_ID, createNotification())
        if (!clipboardListenerRegistered) {
            clipboardManager.addPrimaryClipChangedListener(clipboardListener)
            clipboardListenerRegistered = true
        }
        observeMonitorSetting()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        logger.i("Clipboard", "Service destroyed")
        if (clipboardListenerRegistered) {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
            clipboardListenerRegistered = false
        }
        serviceScope.cancel()
    }

    private fun observeMonitorSetting() {
        if (settingsObserverStarted) return
        settingsObserverStarted = true
        serviceScope.launch {
            preferencesRepository.clipboardMonitorEnabled.collect { isEnabled ->
                if (!isEnabled) {
                    logger.i("Clipboard", "Clipboard monitor disabled, stopping service")
                    stopSelf()
                }
            }
        }
    }

    private fun onClipboardChanged() {
        serviceScope.launch {
            val isEnabled = preferencesRepository.clipboardMonitorEnabled.first()
            if (!isEnabled) return@launch

            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && isValidVideoUrl(text)) {
                    logger.i("Clipboard", "Video URL detected: $text")
                    if (preferencesRepository.autoExtractRecipes.first()) {
                        broadcastRecipeUrl(text)
                    } else {
                        showUrlDetectedNotification(text)
                    }
                }
            }
        }
    }

    private fun isValidVideoUrl(url: String): Boolean {
        val videoPatterns = listOf(
            "youtube.com/watch",
            "youtu.be/",
            "tiktok.com/",
            "instagram.com/",
            "facebook.com/",
            "twitter.com/",
            "x.com/",
            "vimeo.com/",
            "dailymotion.com/"
        )
        return videoPatterns.any { url.contains(it, ignoreCase = true) }
    }

    private fun broadcastRecipeUrl(url: String) {
        val intent = Intent(ACTION_RECIPE_URL_DETECTED).apply {
            putExtra(EXTRA_VIDEO_URL, url)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun showUrlDetectedNotification(url: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("Video URL Detected")
            .setContentText("Tap to extract recipe")
            .setStyle(NotificationCompat.BigTextStyle().bigText(url))
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .addAction(
                android.R.drawable.ic_menu_view,
                "Extract Recipe",
                createPendingIntent()
            )
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(URL_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Clipboard Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors clipboard for recipe video URLs"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("Recipe Grabber Active")
            .setContentText("Monitoring clipboard for recipe videos")
            .setOngoing(true)
            .setContentIntent(createPendingIntent())
            .build()
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val CHANNEL_ID = "clipboard_monitor_channel"
        const val NOTIFICATION_ID = 1
        const val URL_NOTIFICATION_ID = 2
        const val ACTION_RECIPE_URL_DETECTED = "com.recipegrabber.RECIPE_URL_DETECTED"
        const val EXTRA_VIDEO_URL = "video_url"
    }
}
