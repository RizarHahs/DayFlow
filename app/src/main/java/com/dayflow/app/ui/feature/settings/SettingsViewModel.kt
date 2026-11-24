package com.dayflow.app.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.data.prefs.AppPrefs
import android.net.Uri
import com.dayflow.app.data.prefs.NotificationSettings
import com.dayflow.app.domain.model.AppearanceSettings
import com.dayflow.app.domain.model.ThemeMode
import com.dayflow.app.platform.backup.BackupManager
import com.dayflow.app.platform.backup.BackupResult
import com.dayflow.app.domain.repo.AlarmController
import com.dayflow.app.domain.repo.TimerController
import com.dayflow.app.platform.setup.SetupChecker
import com.dayflow.app.platform.setup.SetupStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val checker: SetupChecker,
    private val prefs: AppPrefs,
    private val alarms: AlarmController,
    private val timers: TimerController,
    private val backup: BackupManager,
) : ViewModel() {

    private val _status = MutableStateFlow(checker.check())
    val status: StateFlow<SetupStatus> = _status

    val notifications: StateFlow<NotificationSettings> = prefs.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationSettings())

    val appearance: StateFlow<AppearanceSettings> = prefs.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceSettings())

    private val _message = MutableStateFlow<String?>(null)
    /** Result of the last backup / restore / export, shown under the buttons. */
    val dataMessage: StateFlow<String?> = _message
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun setTheme(v: ThemeMode) { viewModelScope.launch { prefs.setTheme(v) } }
    fun setWeekStart(v: Int) { viewModelScope.launch { prefs.setWeekStart(v) } }
    fun setUse24h(v: Boolean) { viewModelScope.launch { prefs.setUse24h(v) } }
    fun setDefaultTimer(v: Int) { viewModelScope.launch { prefs.setDefaultTimer(v) } }
    fun setReview(v: Boolean) { viewModelScope.launch { prefs.setReview(v) } }
    fun setReviewMinute(v: Int) { viewModelScope.launch { prefs.setReviewMinute(v) } }

    private fun runData(block: suspend () -> BackupResult) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _message.value = when (val r = block()) { is BackupResult.Ok -> r.message; is BackupResult.Failed -> r.reason }
            _busy.value = false
        }
    }
    fun backupTo(uri: Uri) = runData { backup.backupTo(uri) }
    fun restoreFrom(uri: Uri) = runData { backup.restoreFrom(uri) }
    fun exportHabits(uri: Uri) = runData { backup.exportHabitHistoryCsv(uri) }
    fun exportTasks(uri: Uri) = runData { backup.exportTasksCsv(uri) }
    fun exportIdeas(uri: Uri) = runData { backup.exportIdeasMarkdown(uri) }

    /** Re-read the real OS state (call when the screen resumes after the user visits system settings). */
    fun refresh() { _status.value = checker.check() }

    fun setHabits(v: Boolean) { viewModelScope.launch { prefs.setHabits(v) } }
    fun setTasks(v: Boolean) { viewModelScope.launch { prefs.setTasks(v) } }
    fun setActivities(v: Boolean) { viewModelScope.launch { prefs.setActivities(v) } }
    fun setLead(v: Int) { viewModelScope.launch { prefs.setLead(v) } }

    /** @return true if the OS accepted the alarm. */
    fun testAlarm(): Boolean = alarms.scheduleTest(60_000L)

    fun testTimer() { viewModelScope.launch { timers.start("Test timer", 10_000L) } }
}
