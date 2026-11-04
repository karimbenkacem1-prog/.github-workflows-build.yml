package com.karimbenkacem.yourtime

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.karimbenkacem.yourtime.data.*
import com.karimbenkacem.yourtime.logic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

data class UiState(
    val hasAccess: Boolean = false, val todayMs: Long = 0, val sessions: Int = 0, val longestMs: Long = 0,
    val reminders: Int = 0, val week: List<Pair<String, Long>> = emptyList(),
    val apps: List<Pair<String, Long>> = emptyList(), val nextMin: Int? = null,
)

class MainVM(private val app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepo(app)
    private val ur = UsageRepo(app)
    val settings: StateFlow<Prefs?> = repo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui
    val sessionEnd = MutableStateFlow<Long?>(null)

    fun <T> put(k: Preferences.Key<T>, v: T) { viewModelScope.launch { repo.edit { it[k] = v } } }
    fun toggleActivity(id: String) {
        val cur = settings.value?.acts ?: return
        put(K.acts, if (id in cur) cur - id else cur + id)
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!UsageRepo.hasAccess(app)) { _ui.value = UiState(false); return@launch }
            val s = repo.get(); val now = System.currentTimeMillis(); val ds = dayStart()
            if (s.totalOn || s.contOn) TrackerService.start(app) else TrackerService.stop(app)
            try {
                val iv = ur.screenIntervals(ds, now)
                val total = iv.sumOf { it[1] - it[0] }
                val merged = ur.merge(iv, s.breakMin * 60_000L)
                val loc = Locale(s.lang)
                val week = (6 downTo 0).map { d ->
                    val end = if (d == 0) now else dayStart(d - 1)
                    LocalDate.now().minusDays(d.toLong()).dayOfWeek.getDisplayName(TextStyle.SHORT, loc) to ur.totalMs(dayStart(d), end)
                }
                val totalNext = if (s.totalOn) s.totalMin - ((total / 60_000) % s.totalMin).toInt() else null
                val st = if (s.contOn) ur.currentSessionStart(now, s.breakMin * 60_000L) else null
                val contNext = st?.let { s.contMin - (((now - it) / 60_000) % s.contMin).toInt() }
                _ui.value = UiState(
                    true, total, merged.size, merged.maxOfOrNull { it[1] - it[0] } ?: 0,
                    Db.get(app).dao().reminders(LocalDate.now().toString()) ?: 0,
                    week, ur.appUsage(ds, now), listOfNotNull(totalNext, contNext).minOrNull(),
                )
            } catch (e: Exception) { _ui.value = _ui.value.copy(hasAccess = true) }
        }
    }

    fun startSession(min: Int) {
        sessionEnd.value = System.currentTimeMillis() + min * 60_000L
        WorkManager.getInstance(app).enqueueUniqueWork("session", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SessionWorker>().setInitialDelay(min.toLong(), TimeUnit.MINUTES)
                .setInputData(workDataOf("min" to min)).build())
    }
    fun cancelSession() { sessionEnd.value = null; WorkManager.getInstance(app).cancelUniqueWork("session") }
}
