package com.dayflow.app.ui.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.domain.usecase.normalizeTags
import com.dayflow.app.domain.model.IdeaDraft
import com.dayflow.app.domain.model.ItemKind
import com.dayflow.app.domain.repo.IdeaRepository
import com.dayflow.app.domain.usecase.ConvertUseCase
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdeaEditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: IdeaRepository,
    private val convert: ConvertUseCase,
) : ViewModel() {
    val id: Long = handle.get<Long>("id") ?: 0L
    var text by mutableStateOf("")
    var description by mutableStateOf("")
    var tagsText by mutableStateOf("")
    var archived by mutableStateOf(false); private set
    var convertedTo by mutableStateOf<ItemKind?>(null); private set
    var loaded by mutableStateOf(id == 0L); private set

    init {
        if (id != 0L) viewModelScope.launch {
            repo.get(id)?.let {
                text = it.text; description = it.description
                tagsText = it.tags.joinToString(" ") { t -> "#$t" }
                archived = it.archived; convertedTo = it.convertedTo
            }
            loaded = true
        }
    }

    private fun draft() = IdeaDraft(
        id = id, text = text, description = description,
        tags = normalizeTags(tagsText.split(',', ' ', '\n')),
    )

    suspend fun save(): Long = repo.save(draft())
    suspend fun convertTo(kind: ItemKind) { val savedId = save(); convert.ideaTo(savedId, kind) }
    suspend fun toggleArchive() { save(); repo.setArchived(id, !archived) }
    suspend fun delete() { repo.delete(id) }
}

@Composable
fun IdeaEditor(onClose: () -> Unit, modifier: Modifier = Modifier, vm: IdeaEditorViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    SectionTheme(DaySection.IDEAS) {
        if (!vm.loaded) return@SectionTheme
        EditorScaffold(
            title = if (vm.id == 0L) "New idea" else "Idea",
            onBack = onClose,
            onSave = { scope.launch { vm.save(); onClose() } },
            saveEnabled = vm.text.isNotBlank(),
            modifier = modifier,
        ) {
            FieldLabel("The idea")
            DfTextField(vm.text, { vm.text = it }, "Write it down before it slips away", singleLine = false, minLines = 4, textStyle = DfType.serifBody)
            Spacer(Modifier.height(20.dp))
            FieldLabel("More detail")
            DfTextField(vm.description, { vm.description = it }, "Optional", singleLine = false, minLines = 3)
            Spacer(Modifier.height(20.dp))
            FieldLabel("Tags")
            DfTextField(vm.tagsText, { vm.tagsText = it }, "#design #business (space or comma separated)")

            if (vm.id != 0L) {
                Spacer(Modifier.height(28.dp))
                FieldLabel("Turn this idea into")
                vm.convertedTo?.let {
                    Text("Already converted to ${it.name.lowercase()}. Converting again creates another one.", style = DfType.bodySmall, color = Df.colors.textTertiary, modifier = Modifier.padding(bottom = 8.dp))
                }
                val dark = Df.colors.isDark
                ChoiceChips(
                    options = listOf(ItemKind.HABIT, ItemKind.IMPORTANT, ItemKind.NORMAL, ItemKind.EXTRA),
                    selected = null,
                    label = {
                        when (it) { ItemKind.HABIT -> "Daily habit"; ItemKind.IMPORTANT -> "Important task"; ItemKind.NORMAL -> "Target"; else -> "Extra target" }
                    },
                    onSelect = { k -> scope.launch { vm.convertTo(k); onClose() } },
                    color = { k ->
                        sectionPalette(
                            when (k) { ItemKind.HABIT -> DaySection.FOUNDATION; ItemKind.IMPORTANT -> DaySection.IMPORTANT; ItemKind.NORMAL -> DaySection.NORMAL; else -> DaySection.EXTRA },
                            dark,
                        ).accent
                    },
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    if (vm.archived) "Restore from archive" else "Archive",
                    style = DfType.bodyMedium, color = Df.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp))
                        .background(Df.colors.raised).pressable { scope.launch { vm.toggleArchive(); onClose() } }.padding(16.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Delete permanently", style = DfType.bodyMedium, color = PriorityColors.Critical,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp))
                        .pressable { scope.launch { vm.delete(); onClose() } }.padding(16.dp),
                )
            }
        }
    }
}
