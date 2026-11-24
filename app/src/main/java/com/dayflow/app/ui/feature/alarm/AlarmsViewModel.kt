package com.dayflow.app.ui.feature.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Alarm
import com.dayflow.app.domain.model.AlarmStatus
import com.dayflow.app.domain.repo.AlarmController
import com.dayflow.app.platform.setup.SetupChecker
import com.dayflow.app.platform.setup.SetupStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmRow(val alarm: Alarm, val status: AlarmStatus)

data class AlarmsUi(val rows: List<AlarmRow>, val setup: SetupStatus) {
    val next: Alarm? get() = rows.map { it.alarm }.filter { it.enabled && it.nextTriggerAt != null }.minByOrNull { it.nextTriggerAt!! }
}

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    private val controller: AlarmController,
    private val setup: SetupChecker,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)

    /** Status comes from the real OS state, re-read on every change and whenever the screen resumes. */
    val ui: StateFlow<AlarmsUi?> = combine(controller.observeAlarms(), refresh) { list, _ ->
        val s = setup.check()
        AlarmsUi(
            rows = list.map {
                AlarmRow(it, when {
                    !it.enabled -> AlarmStatus.DISABLED
                    !s.alarmsReliable -> AlarmStatus.NEEDS_PERMISSION
                    else -> AlarmStatus.ARMED
                })
            },
            setup = s,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refresh() { refresh.value++ }
    fun toggle(id: Long, enabled: Boolean) { viewModelScope.launch { controller.setEnabled(id, enabled) } }
}
