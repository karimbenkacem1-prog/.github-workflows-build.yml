package com.karimbenkacem.yourtime.logic

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.karimbenkacem.yourtime.MainActivity
import com.karimbenkacem.yourtime.R
import com.karimbenkacem.yourtime.data.*
import java.time.LocalDate
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime

enum class Kind { TOTAL, CONT }

object Reminders {
    private fun sp(c: Context) = c.getSharedPreferences("state", Context.MODE_PRIVATE)

    fun isQuiet(s: Prefs): Boolean {
        if (!s.quietOn) return false
        val n = LocalTime.now(); val m = n.hour * 60 + n.minute
        return if (s.quietStart <= s.quietEnd) m in s.quietStart until s.quietEnd else m >= s.quietStart || m < s.quietEnd
    }

    /**
     * Single evaluation entry point, called by the foreground service (every minute while the screen is on)
     * and by the WorkManager watchdog (every ~15 min). Milestones are persisted per day, so a reminder is
     * never sent twice and counters reset automatically at midnight (the key contains the date).
     */
    private val mu = kotlinx.coroutines.sync.Mutex()
    suspend fun check(c: Context) = mu.withLock { checkInternal(c) }

    private suspend fun checkInternal(c: Context) {
        if (!UsageRepo.hasAccess(c)) return
        val s = SettingsRepo(c).get()
        val repo = UsageRepo(c); val p = sp(c)
        val now = System.currentTimeMillis()

        if (s.totalOn) {
            val total = repo.totalMs(dayStart(), now)
            val m = (total / (s.totalMin * 60_000L)).toInt()
            val key = "tm_${LocalDate.now()}"
            if (m > p.getInt(key, 0)) {
                p.edit().putInt(key, m).apply()
                if (!isQuiet(s)) Notifier.send(c, s, Kind.TOTAL, m * s.totalMin)
            }
        }
        if (s.contOn) {
            val st = repo.currentSessionStart(now, s.breakMin * 60_000L)
            if (st == null) p.edit().putLong("cs", 0).putInt("cm", 0).apply()
            else {
                var last = p.getInt("cm", 0)
                if (p.getLong("cs", 0) != st) { last = 0; p.edit().putLong("cs", st).putInt("cm", 0).apply() }
                val m = ((now - st) / (s.contMin * 60_000L)).toInt()
                if (m > last) {
                    p.edit().putInt("cm", m).apply()
                    if (!isQuiet(s)) Notifier.send(c, s, Kind.CONT, m * s.contMin)
                }
            }
        }
    }
}

object Notifier {
    fun channelId(sound: Boolean, vib: Boolean) = "rem_${if (sound) 1 else 0}${if (vib) 1 else 0}"

    fun createChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        for (sound in listOf(true, false)) for (vib in listOf(true, false)) {
            val ch = NotificationChannel(channelId(sound, vib), "التذكيرات | Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            if (!sound) ch.setSound(null, null)
            ch.enableVibration(vib)
            nm.createNotificationChannel(ch)
        }
        nm.createNotificationChannel(NotificationChannel("svc", "المراقبة | Monitoring", NotificationManager.IMPORTANCE_MIN))
    }

    private fun durAr(min: Int): String {
        if (min % 60 != 0) return "$min دقيقة"
        return when (val h = min / 60) { 1 -> "ساعة"; 2 -> "ساعتان"; in 3..10 -> "$h ساعات"; else -> "$h ساعة" }
    }
    private fun durEn(min: Int): String =
        if (min % 60 != 0) "$min minutes" else (min / 60).let { if (it == 1) "One hour" else "$it hours" }

    suspend fun send(c: Context, s: Prefs, kind: Kind, min: Int) {
        val ar = s.lang == "ar"
        val (title, body) = when {
            kind == Kind.TOTAL && ar -> "وقتك ثمين" to "مرّت ${durAr(min)} من وقتك على الهاتف! 🌿\n\nتذكّر أن وقتك ثمين، فاجعل منه نصيباً للقرآن والعلم والعمل الصالح.\n\nأغلق هاتفك قليلاً، وافتح مصحفك."
            kind == Kind.TOTAL -> "Your Time Is Precious" to "${durEn(min)} have passed on your phone! 🌿\n\nRemember, your time is precious. Give some of it to the Quran, learning, and beneficial activities.\n\nPut your phone down for a while and open your Quran."
            ar -> "وقتك ثمين" to "مرّت ${durAr(min)} من الاستخدام المتواصل.\n\nخذ استراحة من الشاشة، وامنح قلبك بعض الوقت مع القرآن."
            else -> "Your Time Is Precious" to "${durEn(min)} of continuous phone use have passed.\n\nTake a break from the screen and give your heart some time with the Quran."
        }
        post(c, s, title, body, kind.ordinal + 10)
        val dao = Db.get(c).dao(); val d = LocalDate.now().toString()
        dao.put(DayStat(d, (dao.reminders(d) ?: 0) + 1))
    }

    fun post(c: Context, s: Prefs, title: String, body: String, id: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(c, id,
            Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, channelId(s.sound, s.vibrate))
            .setSmallIcon(R.drawable.ic_notif).setColor(0xFF0B6B52.toInt())
            .setContentTitle(title).setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, if (s.lang == "ar") "افتح التطبيق" else "Open app", open)
            .build()
        NotificationManagerCompat.from(c).notify(id, n)
    }
}
