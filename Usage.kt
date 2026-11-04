package com.karimbenkacem.yourtime.logic

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

fun dayStart(back: Int = 0): Long =
    LocalDate.now().minusDays(back.toLong()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

class UsageRepo(private val ctx: Context) {
    private val usm = ctx.getSystemService(UsageStatsManager::class.java)
    private val pm = ctx.getSystemService(PowerManager::class.java)

    companion object {
        fun hasAccess(ctx: Context): Boolean {
            val ops = ctx.getSystemService(AppOpsManager::class.java)
            return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) ==
                AppOpsManager.MODE_ALLOWED
        }
    }

    /** Screen-on (interactive) intervals reconstructed from system usage events. */
    fun screenIntervals(start: Long, end: Long): List<LongArray> {
        val ev = usm.queryEvents(start, end)
        val e = UsageEvents.Event()
        val out = mutableListOf<LongArray>()
        var on = -1L
        var first = true
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            when (e.eventType) {
                15 -> { if (on < 0) on = e.timeStamp; first = false }            // SCREEN_INTERACTIVE
                16 -> {                                                           // SCREEN_NON_INTERACTIVE
                    if (on >= 0) { out += longArrayOf(on, e.timeStamp); on = -1 }
                    else if (first) out += longArrayOf(start, e.timeStamp)       // screen was already on at window start
                    first = false
                }
            }
        }
        if (on >= 0) out += longArrayOf(on, end)
        else if (first && out.isEmpty() && end >= System.currentTimeMillis() - 5000 && pm.isInteractive) out += longArrayOf(start, end)
        return out
    }

    /** Merge intervals separated by less than [gap] ms: those are "continuous sessions". */
    fun merge(iv: List<LongArray>, gap: Long): List<LongArray> {
        val r = mutableListOf<LongArray>()
        for (i in iv) {
            val l = r.lastOrNull()
            if (l != null && i[0] - l[1] < gap) l[1] = i[1] else r += longArrayOf(i[0], i[1])
        }
        return r
    }

    fun totalMs(start: Long, end: Long) = screenIntervals(start, end).sumOf { it[1] - it[0] }

    /** Start time of the current continuous session, or null if the screen is off. */
    fun currentSessionStart(now: Long, gap: Long): Long? {
        if (!pm.isInteractive) return null
        return merge(screenIntervals(now - 12 * 3_600_000L, now), gap).lastOrNull()?.get(0)
    }

    fun appUsage(start: Long, end: Long): List<Pair<String, Long>> {
        val pk = ctx.packageManager
        return usm.queryAndAggregateUsageStats(start, end).values
            .filter { it.totalTimeInForeground > 60_000 && it.packageName != ctx.packageName &&
                pk.getLaunchIntentForPackage(it.packageName) != null }
            .sortedByDescending { it.totalTimeInForeground }.take(8)
            .map { s ->
                val label = try { pk.getApplicationLabel(pk.getApplicationInfo(s.packageName, 0)).toString() }
                catch (e: Exception) { s.packageName }
                label to s.totalTimeInForeground
            }
    }
}
