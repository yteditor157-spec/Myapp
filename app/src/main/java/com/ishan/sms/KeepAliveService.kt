package com.ishan.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** Keeps the app alive on aggressive phones and syncs every 30 seconds. */
class KeepAliveService : Service() {

    @Volatile private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Prefs.key(this) == null) { stopSelf(); return START_NOT_STICKY }
        createChannel()
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
        if (!running) {
            running = true
            Thread {
                while (running) {
                    try {
                        Prefs.key(applicationContext)?.let { Sync.flush(applicationContext, it) }
                        Thread.sleep(30_000)
                    } catch (_: InterruptedException) { break }
                    catch (_: Exception) { }
                }
            }.start()
        }
        return START_STICKY
    }

    override fun onDestroy() { running = false; super.onDestroy() }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "SMS READER", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_sms)
            .setContentTitle("SMS READER চালু আছে")
            .setContentText("bKash / Nagad / Rocket SMS পর্যবেক্ষণ করা হচ্ছে")
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL = "ishan_sms"
        private const val NOTIF_ID = 1001

        fun start(c: Context) {
            try { ContextCompat.startForegroundService(c, Intent(c, KeepAliveService::class.java)) }
            catch (_: Exception) { }
        }
        fun stop(c: Context) { c.stopService(Intent(c, KeepAliveService::class.java)) }
    }
}
