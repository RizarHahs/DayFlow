package com.dayflow.app.ui.feature.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.DayDetail
import com.dayflow.app.domain.repo.HistoryRepository
import com.dayflow.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val history: HistoryRepository,
    clock: DayClock,
) : ViewModel() {
    val today: Int = clock.today()
    private val _day = MutableStateFlow((handle.get<Int>("day") ?: today).coerceAtMost(today))
    val day: StateFlow<Int> = _day

    @OptIn(ExperimentalCoroutinesApi::class)
    val detail: StateFlow<DayDetail?> = _day.flatMapLatest { history.observeDetail(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var note by mutableStateOf(""); private set
    private var noteLoadedFor = Int.MIN_VALUE

    // Ordered, unbounded: every keystroke is saved in sequence, none dropped.
    private val saves = Channel<Pair<Int, String>>(Channel.UNLIMITED)

    init {
        viewModelScope.launch { for ((d, t) in saves) history.saveNote(d, t) }
        loadNote(_day.value)
    }

    private fun loadNote(d: Int) {
        viewModelScope.launch {
            val saved = history.observeDetail(d).first().note
            if (_day.value == d) { note = saved; noteLoadedFor = d }
        }
    }

    fun shift(delta: Int) {
        val target = (_day.value + delta).coerceAtMost(today)
        if (target == _day.value) return
        _day.value = target
        note = ""
        noteLoadedFor = Int.MIN_VALUE
        loadNote(target)
    }

    fun onNoteChange(text: String) {
        val d = _day.value
        if (noteLoadedFor != d) return   // ignore typing before the saved note has loaded
        note = text
        saves.trySend(d to text)
    }
}
