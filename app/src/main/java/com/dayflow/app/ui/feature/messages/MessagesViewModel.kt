package com.dayflow.app.ui.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.domain.model.MessageSettings
import com.dayflow.app.domain.model.MessageStatus
import com.dayflow.app.domain.model.ScheduledMessage
import com.dayflow.app.domain.repo.ScheduledMessageController
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.platform.setup.SetupChecker
import com.dayflow.app.platform.whatsapp.ScheduledMessageScheduler
import com.dayflow.app.platform.whatsapp.WhatsAppAvailability
import com.dayflow.app.platform.whatsapp.WhatsAppLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUi(
    val today: Int,
    val needsAction: List<ScheduledMessage>,   // Triggered or Failed: ready to send / needs a retry
    val upcoming: List<ScheduledMessage>,      // Scheduled
    val opened: List<ScheduledMessage>,
    val settings: MessageSettings,
    val exactAlarmOk: Boolean,
    val whatsApp: WhatsAppAvailability,
) {
    val isEmpty: Boolean get() = needsAction.isEmpty() && upcoming.isEmpty() && opened.isEmpty()
}

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val controller: ScheduledMessageController,
    private val prefs: AppPrefs,
    private val setup: SetupChecker,
    private val launcher: WhatsAppLauncher,
    private val scheduler: ScheduledMessageScheduler,
    clock: DayClock,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)

    /** Permission / WhatsApp state is re-read on every resume, so the warnings always reflect reality. */
    val ui: StateFlow<MessagesUi?> = combine(controller.observe(), prefs.messageSettings, refresh, clock.todayFlow) { list, settings, _, today ->
        MessagesUi(
            today = today,
            needsAction = list.filter { it.status == MessageStatus.TRIGGERED || it.status == MessageStatus.FAILED }
                .sortedByDescending { it.triggeredAt ?: 0L },
            upcoming = list.filter { it.status == MessageStatus.SCHEDULED },
            opened = list.filter { it.status == MessageStatus.OPENED }.sortedByDescending { it.openedAt ?: 0L },
            settings = settings,
            exactAlarmOk = setup.canScheduleExact(),
            whatsApp = launcher.availability(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refresh() { refresh.value++ }

    fun delete(id: Long) { viewModelScope.launch { controller.delete(id) } }

    /** Changing the mode re-arms every pending message with the right AlarmManager call. */
    fun setAttentionMode(v: Boolean) {
        viewModelScope.launch {
            prefs.setAttentionMode(v)
            scheduler.rescheduleAll()
        }
    }

    fun setDefaultCountryCode(v: String) { viewModelScope.launch { prefs.setDefaultCountryCode(v) } }
}
