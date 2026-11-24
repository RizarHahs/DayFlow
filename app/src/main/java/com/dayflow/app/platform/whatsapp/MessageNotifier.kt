package com.dayflow.app.platform.whatsapp

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dayflow.app.R
import com.dayflow.app.platform.Channels
import com.dayflow.app.platform.PendingIntents
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The normal (non-Attention) alert for a scheduled message. One notification per message, keyed by its id,
 * so a repeated delivery replaces instead of duplicating. Tapping it, or its "Open WhatsApp" button, goes
 * directly to OpenWhatsAppActivity (no trampoline), which opens the chat with the text prefilled.
 */
@Singleton
class MessageNotifier @Inject constructor(@ApplicationContext private val ctx: Context) {

    /** @return false if notifications are blocked, so the caller can record that no alert could be shown. */
    fun postRaw(id: Long, title: String, sub: String, body: String): Boolean {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return false
        val open = PendingIntents.openWhatsApp(ctx, id)
        val n = NotificationCompat.Builder(ctx, Channels.MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(sub)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setSummaryText(sub))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)   // message text stays hidden on the lock screen
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "Open WhatsApp", open)
            .build()
        return try {
            nm.notify(TAG, id.toInt(), n)
            true
        } catch (_: SecurityException) {
            false   // POST_NOTIFICATIONS revoked between the check and the call
        }
    }

    fun cancel(id: Long) {
        NotificationManagerCompat.from(ctx).cancel(TAG, id.toInt())
    }

    companion object { private const val TAG = "wa_message" }
}
