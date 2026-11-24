package com.dayflow.app.ui.feature.messages

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.domain.model.MessageSaveOutcome
import com.dayflow.app.domain.model.MessageSettings
import com.dayflow.app.domain.model.ScheduledMessageDraft
import com.dayflow.app.domain.repo.ScheduledMessageController
import com.dayflow.app.domain.time.DayClock
import com.dayflow.app.domain.time.dayLongLabel
import com.dayflow.app.domain.usecase.MessageValidation
import com.dayflow.app.domain.usecase.Occasions
import com.dayflow.app.domain.usecase.PhoneNumber
import com.dayflow.app.platform.setup.SetupChecker
import com.dayflow.app.platform.setup.SetupIntents
import com.dayflow.app.ui.components.*
import com.dayflow.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MessageEditorViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val controller: ScheduledMessageController,
    prefs: AppPrefs,
    private val setup: SetupChecker,
    clock: DayClock,
) : ViewModel() {
    val id: Long = handle.get<Long>("id") ?: 0L
    val today: Int = clock.today()

    var draft by mutableStateOf(
        ScheduledMessageDraft(day = today, occasion = "Birthday", message = Occasions.templateFor("Birthday").orEmpty())
    ); private set
    /** True while "Custom" is selected, so the occasion text box is shown even when it is still empty. */
    var customOccasion by mutableStateOf(false); private set
    var loaded by mutableStateOf(id == 0L); private set
    var exactOk by mutableStateOf(setup.canScheduleExact()); private set
    var serverProblems by mutableStateOf<List<String>>(emptyList()); private set

    val settings: StateFlow<MessageSettings> = prefs.messageSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessageSettings())

    init {
        if (id != 0L) viewModelScope.launch {
            controller.getDraft(id)?.let {
                draft = it
                customOccasion = !Occasions.isPreset(it.occasion)
            }
            loaded = true
        }
    }

    fun refreshPermissions() { exactOk = setup.canScheduleExact() }
    fun edit(block: (ScheduledMessageDraft) -> ScheduledMessageDraft) { draft = block(draft); serverProblems = emptyList() }

    /** Picking an occasion fills in a starter message, but never overwrites text the user wrote. */
    fun selectOccasion(label: String) {
        val old = Occasions.templateFor(draft.occasion)
        if (label == Occasions.CUSTOM) {
            customOccasion = true
            edit { it.copy(occasion = "") }
            return
        }
        customOccasion = false
        val fresh = Occasions.templateFor(label).orEmpty()
        edit { it.copy(occasion = label, message = if (it.message.isBlank() || it.message == old) fresh else it.message) }
    }

    suspend fun save(): MessageSaveOutcome {
        val outcome = controller.save(draft)
        if (outcome is MessageSaveOutcome.Invalid) serverProblems = outcome.problems
        return outcome
    }

    suspend fun delete() { controller.delete(id) }
}

@Composable
fun MessageEditor(onClose: () -> Unit, modifier: Modifier = Modifier, vm: MessageEditorViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val d = vm.draft
    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshPermissions() }

    // Android's own contact picker: returns just the chosen row, so no READ_CONTACTS permission is needed.
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.data
        if (r.resultCode == Activity.RESULT_OK && uri != null) {
            runCatching {
                ctx.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                    null, null, null,
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val number = c.getString(0).orEmpty()
                        val name = c.getString(1).orEmpty()
                        vm.edit { it.copy(phone = number, recipientName = name.ifBlank { it.recipientName }) }
                    }
                }
            }
        }
    }

    SectionTheme(DaySection.NORMAL) {
        if (!vm.loaded) return@SectionTheme
        val problems = MessageValidation.validate(d, System.currentTimeMillis(), ZoneId.systemDefault(), settings.defaultCountryCode)
        val normalized = PhoneNumber.normalize(d.phone, settings.defaultCountryCode)
        EditorScaffold(
            title = if (vm.id == 0L) "New scheduled message" else "Edit message",
            onBack = onClose,
            onSave = {
                scope.launch {
                    val outcome = vm.save()
                    if (outcome is MessageSaveOutcome.Saved) {
                        Toast.makeText(
                            ctx,
                            if (outcome.exactAlarmOk) "Scheduled. You'll be alerted at the exact time."
                            else "Scheduled, but exact alarms are off, so the alert may arrive a little late.",
                            Toast.LENGTH_LONG,
                        ).show()
                        onClose()
                    }
                }
            },
            saveEnabled = problems.isEmpty(),
            modifier = modifier,
        ) {
            NotificationHint()
            if (!vm.exactOk) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PriorityColors.High.copy(alpha = 0.14f)).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Exact alarms are off: the alert may arrive late.", style = DfType.bodySmall, color = Df.colors.textPrimary, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                    Text(
                        "Allow", style = DfType.bodyMedium, color = PriorityColors.High,
                        modifier = Modifier.heightIn(min = 44.dp).pressable { SetupIntents.launch(ctx, SetupIntents.exactAlarm(ctx)) }.padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            FieldLabel("Occasion")
            ChoiceChips(Occasions.labels, if (vm.customOccasion) Occasions.CUSTOM else d.occasion, { it }, vm::selectOccasion)
            if (vm.customOccasion) {
                Spacer(Modifier.height(10.dp))
                DfTextField(d.occasion, { v -> vm.edit { it.copy(occasion = v.take(40)) } }, "e.g. Mum's retirement")
            }
            Spacer(Modifier.height(24.dp))

            FieldLabel("Recipient")
            DfTextField(d.recipientName, { v -> vm.edit { it.copy(recipientName = v.take(60)) } }, "Name")
            Spacer(Modifier.height(10.dp))
            DfTextField(d.phone, { v -> vm.edit { it.copy(phone = v.take(30)) } }, "WhatsApp number, e.g. +977 9812345678", keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(6.dp))
            when {
                d.phone.isBlank() -> Text("Use the international format with the country code.", style = DfType.bodySmall, color = Df.colors.textTertiary)
                normalized == null -> Text("Add the country code, like +977 9812345678 (8 to 15 digits).", style = DfType.bodySmall, color = PriorityColors.High)
                else -> Text("Will open the chat with $normalized", style = DfType.mono, color = Df.section.accent)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Pick from contacts", style = DfType.bodyMedium, color = Df.section.accent,
                modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).pressable {
                    try {
                        contactPicker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(ctx, "No contacts app found. Type the number instead.", Toast.LENGTH_SHORT).show()
                    }
                }.padding(horizontal = 4.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(20.dp))

            FieldLabel("Date")
            DayField(d.day, vm.today, { day -> vm.edit { it.copy(day = day) } })
            Spacer(Modifier.height(16.dp))
            FieldLabel("Exact time")
            TimeField(d.minute, { m -> if (m != null) vm.edit { it.copy(minute = m) } }, defaultMinute = d.minute, clearable = false)
            Spacer(Modifier.height(6.dp))
            Text(dayLongLabel(d.day) + " at " + formatMinute(d.minute), style = DfType.bodySmall, color = Df.colors.textTertiary)
            Spacer(Modifier.height(24.dp))

            FieldLabel("Message")
            DfTextField(d.message, { v -> vm.edit { it.copy(message = v.take(MessageValidation.MAX_LENGTH + 200)) } }, "Write your message. Emojis and line breaks are kept.", singleLine = false, minLines = 4)
            Spacer(Modifier.height(6.dp))
            Text(
                "${d.message.length} / ${MessageValidation.MAX_LENGTH}",
                style = DfType.mono, color = if (d.message.length > MessageValidation.MAX_LENGTH) PriorityColors.Critical else Df.colors.textTertiary,
            )
            Spacer(Modifier.height(20.dp))

            FieldLabel("Preview in WhatsApp")
            Preview(d.message, normalized, d.recipientName)

            val shown = (problems + vm.serverProblems).distinct()
            if (shown.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                shown.forEach { Text("• $it", style = DfType.bodySmall, color = PriorityColors.High, modifier = Modifier.padding(vertical = 2.dp)) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "At the time you choose, DAYFLOW alerts you and opens this chat with the message ready. You press Send yourself.",
                style = DfType.bodySmall, color = Df.colors.textTertiary,
            )

            if (vm.id != 0L) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Delete scheduled message", style = DfType.bodyMedium, color = PriorityColors.Critical,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp))
                        .pressable { confirmDelete = true }.padding(vertical = 14.dp),
                )
            }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                containerColor = Df.colors.surface,
                title = { Text("Delete this message?", style = DfType.title, color = Df.colors.textPrimary) },
                text = { Text("The reminder will be cancelled and removed.", style = DfType.body, color = Df.colors.textSecondary) },
                confirmButton = { TextButton(onClick = { scope.launch { vm.delete(); onClose() } }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            )
        }
    }
}

/** A chat-bubble preview in the app's own colours. It shows what will be prefilled; nothing is sent. */
@Composable
private fun Preview(message: String, phone: String?, name: String) {
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Df.colors.raised).padding(16.dp)) {
        Text(
            "To: " + (name.ifBlank { phone ?: "..." }),
            style = DfType.bodySmall, color = Df.colors.textSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.align(Alignment.End).widthIn(max = 300.dp).clip(shape).background(Df.section.soft)
                .border(1.dp, Df.section.accent.copy(alpha = 0.4f), shape).padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                message.ifBlank { "Your message appears here." },
                style = DfType.body, color = if (message.isBlank()) Df.colors.textTertiary else Df.colors.textPrimary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("Ready in the chat box. You press Send.", style = DfType.bodySmall, color = Df.colors.textTertiary, modifier = Modifier.align(Alignment.End))
    }
}
