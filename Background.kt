package com.karimbenkacem.yourtime.logic

import android.app.Notification
import android.app.Service
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.karimbenkacem.yourtime.R
import com.karimbenkacem.yourtime.data.SettingsRepo
import kotlinx.coroutines.*

/** Lightweight foreground service: only does work while the screen is ON (one cheap query per minute). */
class TrackerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null
    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == Intent.ACTION_SCREEN_ON) startTicker() else ticker?.cancel()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        val ar = java.util.Locale.getDefault().language == "ar"
        val n: Notification = NotificationCompat.Builder(this, "svc").setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (ar) "وقتك ثمين" else "Your Time Is Precious")
            .setContentText(if (ar) "التذكيرات تعمل بهدوء" else "Reminders are active")
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_MIN).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        registerReceiver(rx, IntentFilter().apply { addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF) })
        if (getSystemService(PowerManager::class.java).isInteractive) startTicker()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch { while (isActive) { runCatching { Reminders.check(applicationContext) }; delay(60_000) } }
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onDestroy() { runCatching { unregisterReceiver(rx) }; scope.cancel(); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        fun start(c: Context) { runCatching { ContextCompat.startForegroundService(c, Intent(c, TrackerService::class.java)) } }
        fun stop(c: Context) { c.stopService(Intent(c, TrackerService::class.java)) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED && UsageRepo.hasAccess(c)) TrackerService.start(c)
    }
}

/** ~15-minute watchdog: still delivers reminders if the OS killed the foreground service. */
class CheckWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result { runCatching { Reminders.check(applicationContext) }; return Result.success() }
}

/** Fires when a "beneficial activity" session finishes. */
class SessionWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result {
        val s = SettingsRepo(applicationContext).get()
        val min = inputData.getInt("min", 10)
        if (s.lang == "ar") Notifier.post(applicationContext, s, "تقبّل الله منك 🌿", "أتممت $min دقيقة في ما ينفعك. بارك الله في وقتك.", 30)
        else Notifier.post(applicationContext, s, "May Allah accept it 🌿", "You completed $min minutes of something beneficial. May Allah bless your time.", 30)
        return Result.success()
    }
}
