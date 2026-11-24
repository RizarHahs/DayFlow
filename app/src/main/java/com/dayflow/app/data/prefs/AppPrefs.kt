package com.dayflow.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dayflow.app.domain.model.AppearanceSettings
import com.dayflow.app.domain.model.MessageSettings
import com.dayflow.app.domain.model.ThemeMode
import com.dayflow.app.domain.usecase.ReminderCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.store by preferencesDataStore("dayflow_prefs")

data class NotificationSettings(
    val habits: Boolean = true,
    val tasks: Boolean = true,
    val activities: Boolean = true,
    val activityLeadMin: Int = 5,
    val review: Boolean = true,
    val reviewMinute: Int = 21 * 60 + 30,
) {
    val enabledCategories: Set<ReminderCategory>
        get() = buildSet {
            if (habits) add(ReminderCategory.HABIT)
            if (tasks) add(ReminderCategory.TASK)
            if (activities) add(ReminderCategory.ACTIVITY)
            if (review) add(ReminderCategory.REVIEW)
        }
}

/** Lightweight preferences (DataStore). Phase 5 grows this into the full Settings screen. */
@Singleton
class AppPrefs @Inject constructor(@ApplicationContext private val ctx: Context) {
    private object K {
        val habits = booleanPreferencesKey("notify_habits")
        val tasks = booleanPreferencesKey("notify_tasks")
        val activities = booleanPreferencesKey("notify_activities")
        val lead = intPreferencesKey("activity_lead_min")
        val posted = stringSetPreferencesKey("posted_reminders")
        val review = booleanPreferencesKey("notify_review")
        val reviewMinute = intPreferencesKey("review_minute")
        val theme = stringPreferencesKey("theme")
        val weekStart = intPreferencesKey("week_start")
        val use24h = booleanPreferencesKey("use_24h")
        val defaultTimer = intPreferencesKey("default_timer_min")
        val lastFinalized = intPreferencesKey("last_finalized_day")
        val attention = booleanPreferencesKey("msg_attention_mode")
        val countryCode = stringPreferencesKey("msg_default_country_code")
    }

    val notifications: Flow<NotificationSettings> = ctx.store.data.map { p ->
        NotificationSettings(
            habits = p[K.habits] ?: true,
            tasks = p[K.tasks] ?: true,
            activities = p[K.activities] ?: true,
            activityLeadMin = p[K.lead] ?: 5,
            review = p[K.review] ?: true,
            reviewMinute = p[K.reviewMinute] ?: (21 * 60 + 30),
        )
    }

    val appearance: Flow<AppearanceSettings> = ctx.store.data.map { p ->
        AppearanceSettings(
            theme = runCatching { ThemeMode.valueOf(p[K.theme] ?: "") }.getOrDefault(ThemeMode.DARK),
            weekStart = (p[K.weekStart] ?: 1).coerceIn(1, 7),
            use24h = p[K.use24h] ?: true,
            defaultTimerMin = (p[K.defaultTimer] ?: 25).coerceIn(1, 999),
        )
    }

    suspend fun notificationsNow(): NotificationSettings = notifications.first()

    suspend fun setHabits(v: Boolean) { ctx.store.edit { it[K.habits] = v } }
    suspend fun setTasks(v: Boolean) { ctx.store.edit { it[K.tasks] = v } }
    suspend fun setActivities(v: Boolean) { ctx.store.edit { it[K.activities] = v } }
    suspend fun setLead(v: Int) { ctx.store.edit { it[K.lead] = v } }
    suspend fun setReview(v: Boolean) { ctx.store.edit { it[K.review] = v } }
    suspend fun setReviewMinute(v: Int) { ctx.store.edit { it[K.reviewMinute] = v } }
    suspend fun setTheme(v: ThemeMode) { ctx.store.edit { it[K.theme] = v.name } }
    suspend fun setWeekStart(v: Int) { ctx.store.edit { it[K.weekStart] = v } }
    suspend fun setUse24h(v: Boolean) { ctx.store.edit { it[K.use24h] = v } }
    suspend fun setDefaultTimer(v: Int) { ctx.store.edit { it[K.defaultTimer] = v } }
    suspend fun appearanceNow(): AppearanceSettings = appearance.first()

    val messageSettings: Flow<MessageSettings> = ctx.store.data.map { p ->
        MessageSettings(attentionMode = p[K.attention] ?: false, defaultCountryCode = p[K.countryCode] ?: "")
    }
    suspend fun messageSettingsNow(): MessageSettings = messageSettings.first()
    suspend fun setAttentionMode(v: Boolean) { ctx.store.edit { it[K.attention] = v } }
    suspend fun setDefaultCountryCode(v: String) { ctx.store.edit { it[K.countryCode] = v.trim() } }

    suspend fun lastFinalizedDay(): Int? = ctx.store.data.first()[K.lastFinalized]
    suspend fun setLastFinalizedDay(day: Int) { ctx.store.edit { it[K.lastFinalized] = day } }

    suspend fun postedKeys(): Set<String> = ctx.store.data.first()[K.posted] ?: emptySet()

    /** Remembers which reminders were already shown; forgets keys older than yesterday (key ends with the day). */
    suspend fun markPosted(keys: Collection<String>, today: Int) {
        ctx.store.edit { p ->
            val merged = (p[K.posted] ?: emptySet()) + keys
            p[K.posted] = merged.filter { (it.substringAfterLast(':').toIntOrNull() ?: today) >= today - 1 }.toSet()
        }
    }
}
