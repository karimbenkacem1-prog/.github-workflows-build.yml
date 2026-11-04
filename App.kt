package com.karimbenkacem.yourtime

import android.app.Application
import androidx.work.*
import com.karimbenkacem.yourtime.logic.CheckWorker
import com.karimbenkacem.yourtime.logic.Notifier
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "check", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CheckWorker>(15, TimeUnit.MINUTES).build())
    }
}
