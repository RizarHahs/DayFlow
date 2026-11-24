package com.dayflow.app.ui.feature.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.model.Idea
import com.dayflow.app.domain.model.IdeaDraft
import com.dayflow.app.domain.repo.IdeaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IdeasUi(
    val query: String,
    val tag: String?,
    val showArchived: Boolean,
    val allTags: List<String>,
    val pinned: List<Idea>,
    val others: List<Idea>,
    val total: Int,
)

@HiltViewModel
class IdeasViewModel @Inject constructor(private val repo: IdeaRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val tag = MutableStateFlow<String?>(null)
    private val archived = MutableStateFlow(false)

    val ui: StateFlow<IdeasUi> = combine(repo.observeAll(), query, tag, archived) { all, q, t, arch ->
        val scoped = all.filter { it.archived == arch }
        val needle = q.trim().lowercase()
        val filtered = scoped.filter { i ->
            (t == null || t in i.tags) &&
                (needle.isEmpty() ||
                    i.text.lowercase().contains(needle) ||
                    i.description.lowercase().contains(needle) ||
                    i.tags.any { it.contains(needle.removePrefix("#")) })
        }
        IdeasUi(
            query = q, tag = t, showArchived = arch,
            allTags = scoped.flatMap { it.tags }.distinct().sorted(),
            pinned = filtered.filter { it.pinned },
            others = filtered.filter { !it.pinned },
            total = scoped.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IdeasUi("", null, false, emptyList(), emptyList(), emptyList(), 0))

    fun setQuery(q: String) { query.value = q }
    fun setTag(t: String?) { tag.value = if (tag.value == t) null else t }
    fun setArchived(a: Boolean) { archived.value = a; tag.value = null }
    fun togglePin(id: Long) { viewModelScope.launch { repo.togglePin(id) } }
    fun archive(id: Long, value: Boolean) { viewModelScope.launch { repo.setArchived(id, value) } }
    fun quickAdd(text: String) { viewModelScope.launch { if (text.isNotBlank()) repo.save(IdeaDraft(text = text)) } }
}
