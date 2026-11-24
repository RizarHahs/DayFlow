package com.dayflow.app.platform.setup

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.dayflow.app.platform.Channels
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SetupStatus(
    val sdk: Int,
    val androidRelease: String,
    val manufacturer: String,
    val model: String,
    val notifications: Boolean,
    val needsRuntimeNotificationPermission: Boolean,
    val exactAlarms: Boolean,
    val alarmChannelOk: Boolean,
    val fullScreen: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val isOnePlus: Boolean get() = manufacturer.equals("OnePlus", ignoreCase = true)

    /** Everything an alarm genuinely depends on. Battery/vendor settings are advisory (cannot be verified). */
    val alarmsReliable: Boolean get() = notifications && exactAlarms && alarmChannelOk
    val attentionCount: Int
        get() = listOf(notifications, exactAlarms, alarmChannelOk, fullScreen, batteryUnrestricted).count { !it }
}

/** Reads the real permission/settings state. Never assumes anything is granted. */
@Singleton
class SetupChecker @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ctx.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
    }

    fun check(): SetupStatus {
        val nm = ctx.getSystemService<NotificationManager>()
        val pm = ctx.getSystemService<PowerManager>()
        val channelOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm?.getNotificationChannel(Channels.ALARMS)?.importance != NotificationManager.IMPORTANCE_NONE
        } else true
        val fullScreen = if (Build.VERSION.SDK_INT >= 34) nm?.canUseFullScreenIntent() == true else true
        return SetupStatus(
            sdk = Build.VERSION.SDK_INT,
            androidRelease = Build.VERSION.RELEASE ?: "?",
            manufacturer = Build.MANUFACTURER ?: "",
            model = Build.MODEL ?: "",
            notifications = NotificationManagerCompat.from(ctx).areNotificationsEnabled(),
            needsRuntimeNotificationPermission = Build.VERSION.SDK_INT >= 33,
            exactAlarms = canScheduleExact(),
            alarmChannelOk = channelOk,
            fullScreen = fullScreen,
            batteryUnrestricted = pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true,
        )
    }
}

/** Settings deep links. Each has a fallback because OEM builds differ; nothing here bypasses restrictions. */
object SetupIntents {
    private fun appDetails(ctx: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))

    fun appNotificationSettings(ctx: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)

    fun alarmChannelSettings(ctx: Context) =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, Channels.ALARMS)

    fun exactAlarm(ctx: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", ctx.packageName, null)) else null

    fun fullScreen(ctx: Context): Intent? =
        if (Build.VERSION.SDK_INT >= 34)
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.fromParts("package", ctx.packageName, null)) else null

    /** Direct allow-dialog (permitted for alarm apps), with the generic list as fallback. */
    fun batteryRequest(ctx: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.fromParts("package", ctx.packageName, null))

    fun batteryList() = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** Best effort: OnePlus "Auto-launch" screen. May not exist on every OxygenOS build. */
    fun onePlusAutoLaunch() = Intent().setComponent(
        ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
    )

    fun appInfo(ctx: Context) = appDetails(ctx)

    /** Tries [primary], then each fallback, then app info. Returns false only if nothing could open. */
    fun launch(ctx: Context, primary: Intent?, vararg fallbacks: Intent): Boolean {
        val chain = listOfNotNull(primary) + fallbacks + appDetails(ctx)
        for (i in chain) {
            try {
                ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: Exception) { /* ActivityNotFound / Security: try the next one */ }
        }
        return false
    }
}
