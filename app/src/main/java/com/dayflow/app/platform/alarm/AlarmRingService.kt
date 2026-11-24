package com.dayflow.app.platform.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.dayflow.app.R
import com.dayflow.app.platform.Actions
import com.dayflow.app.platform.Channels
import com.dayflow.app.platform.PendingIntents
import com.dayflow.app.platform.whatsapp.MessageNotifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that rings an alarm: plays on the ALARM audio stream (so it works in silent/
 * vibrate ringer mode and follows the alarm volume), vibrates, and shows a full-screen notification.
 * Stops on Dismiss, snoozes on Snooze, and gives up after [RING_TIMEOUT_MS] by posting "Missed alarm".
 */
@AndroidEntryPoint
class AlarmRingService : Service() {

    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var messageNotifier: MessageNotifier

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var alarmId = 0L
    private var label = ""
    private var minute = 0
    private var snoozeMin = 9
    private var ringing = false

    // Attention Mode for a scheduled WhatsApp message (messageId < 0 = this is a normal alarm).
    private var messageId = NO_MESSAGE
    private var messageSub = ""
    private var messageBody = ""
    private val timeout = Runnable { onTimeout() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.ALARM_DISMISS -> {
                // A dismiss aimed at one message must not silence a different alarm that happens to be ringing.
                val target = intent?.getLongExtra(Actions.EXTRA_MESSAGE_ID, NO_TARGET) ?: NO_TARGET
                if (target == NO_TARGET || (ringing && target == messageId)) stopRinging()
                else if (!ringing) stopSelf()
            }
            Actions.ALARM_SNOOZE -> { scheduler.scheduleSnooze(alarmId, snoozeMin); stopRinging() }
            else -> if (intent != null) startRinging(intent) else stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRinging(i: Intent) {
        stopPlayback()   // a newer alarm replaces one that is still ringing
        ringing = true
        messageId = i.getLongExtra(Actions.EXTRA_MESSAGE_ID, NO_MESSAGE)
        messageSub = i.getStringExtra(Actions.EXTRA_SUBTEXT) ?: ""
        messageBody = i.getStringExtra(Actions.EXTRA_TEXT) ?: ""
        alarmId = i.getLongExtra(Actions.EXTRA_ALARM_ID, 0)
        label = i.getStringExtra(Actions.EXTRA_LABEL) ?: "Alarm"
        minute = i.getIntExtra(Actions.EXTRA_MINUTE, 0)
        snoozeMin = i.getIntExtra(Actions.EXTRA_SNOOZE_MIN, 9)
        val sound = i.getStringExtra(Actions.EXTRA_SOUND)
        val vibrate = i.getBooleanExtra(Actions.EXTRA_VIBRATE, true)

        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        playSound(sound)
        if (vibrate) startVibration()
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, RING_TIMEOUT_MS)
    }

    private fun buildMessageNotification(): android.app.Notification {
        val open = PendingIntents.openWhatsApp(this, messageId)
        val fullScreen = PendingIntent.getActivity(
            this, 2, AlarmRingActivity.messageIntent(this, messageId, label, messageSub, messageBody),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, Channels.ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label)
            .setContentText(messageSub)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(open)
            .addAction(0, "Open WhatsApp", open)
            .addAction(0, "Dismiss", serviceAction(Actions.ALARM_DISMISS))
            .build()
    }

    private fun buildNotification() = run {
        if (messageId != NO_MESSAGE) return@run buildMessageNotification()
        val time = "%02d:%02d".format(minute / 60, minute % 60)
        val fullScreen = PendingIntent.getActivity(
            this, 1, AlarmRingActivity.intent(this, label, time, snoozeMin),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        NotificationCompat.Builder(this, Channels.ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label)
            .setContentText(time)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "Snooze $snoozeMin min", serviceAction(Actions.ALARM_SNOOZE))
            .addAction(0, "Dismiss", serviceAction(Actions.ALARM_DISMISS))
            .build()
    }

    private fun serviceAction(action: String) = PendingIntent.getService(
        this, action.hashCode().and(0xFFFF),
        Intent(this, AlarmRingService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun playSound(uriString: String?) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val candidates = listOfNotNull(
            uriString?.let { Uri.parse(it) },
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        for (uri in candidates) {
            try {
                player = MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(this@AlarmRingService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
                return
            } catch (_: Exception) {
                player?.release(); player = null   // unreadable sound (e.g. removed): try the next fallback
            }
        }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private fun startVibration() {
        val v = vibrator() ?: return
        val pattern = longArrayOf(0, 600, 400, 600, 400)
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pattern, 0))
        else @Suppress("DEPRECATION") v.vibrate(pattern, 0)
    }

    private fun stopPlayback() {
        handler.removeCallbacks(timeout)
        player?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        player = null
        vibrator()?.cancel()
    }

    private fun stopRinging() {
        ringing = false
        stopPlayback()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        sendBroadcast(Intent(Actions.ALARM_STOPPED).setPackage(packageName))
        stopSelf()
    }

    /** Nobody answered: stop making noise and leave a visible record. */
    private fun onTimeout() {
        if (messageId != NO_MESSAGE) {
            // Nobody reacted: stop the noise but keep a normal, tappable notification so the message isn't lost.
            val id = messageId; val title = label; val sub = messageSub; val body = messageBody
            stopRinging()
            messageNotifier.postRaw(id, title, sub, body)
            return
        }
        val time = "%02d:%02d".format(minute / 60, minute % 60)
        stopRinging()
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            val n = NotificationCompat.Builder(this, Channels.ALARMS_MISSED)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Missed alarm")
                .setContentText("$label · $time rang but wasn't answered")
                .setContentIntent(PendingIntents.openApp(this))
                .setAutoCancel(true)
                .build()
            try { NotificationManagerCompat.from(this).notify(MISSED_ID, n) } catch (_: SecurityException) {}
        }
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 7001
        private const val MISSED_ID = 7099
        private const val RING_TIMEOUT_MS = 5 * 60_000L
        private const val NO_MESSAGE = -1L
        private const val NO_TARGET = Long.MIN_VALUE

        fun startIntent(ctx: Context, id: Long, label: String, minute: Int, sound: String?, vibrate: Boolean, snoozeMin: Int) =
            Intent(ctx, AlarmRingService::class.java).setAction(Actions.ALARM_START_RING)
                .putExtra(Actions.EXTRA_ALARM_ID, id)
                .putExtra(Actions.EXTRA_LABEL, label)
                .putExtra(Actions.EXTRA_MINUTE, minute)
                .putExtra(Actions.EXTRA_SOUND, sound)
                .putExtra(Actions.EXTRA_VIBRATE, vibrate)
                .putExtra(Actions.EXTRA_SNOOZE_MIN, snoozeMin)

        fun actionIntent(ctx: Context, action: String) = Intent(ctx, AlarmRingService::class.java).setAction(action)

        /** Ring for a scheduled WhatsApp message (Attention Mode). Same sound/vibration/full-screen pipeline as alarms. */
        fun messageIntent(ctx: Context, messageId: Long, title: String, sub: String, body: String) =
            Intent(ctx, AlarmRingService::class.java).setAction(Actions.ALARM_START_RING)
                .putExtra(Actions.EXTRA_MESSAGE_ID, messageId)
                .putExtra(Actions.EXTRA_LABEL, title)
                .putExtra(Actions.EXTRA_SUBTEXT, sub)
                .putExtra(Actions.EXTRA_TEXT, body)
                .putExtra(Actions.EXTRA_VIBRATE, true)

        /** Stops the ring only if it is currently ringing for [messageId]. */
        fun dismissMessageIntent(ctx: Context, messageId: Long) =
            actionIntent(ctx, Actions.ALARM_DISMISS).putExtra(Actions.EXTRA_MESSAGE_ID, messageId)
    }
}
