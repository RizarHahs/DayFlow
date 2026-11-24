package com.dayflow.app.platform.whatsapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.dayflow.app.domain.usecase.WhatsAppLink
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class WhatsAppTarget(val pkg: String, val label: String) {
    BUSINESS("com.whatsapp.w4b", "WhatsApp Business"),
    REGULAR("com.whatsapp", "WhatsApp"),
}

data class WhatsAppAvailability(val business: Boolean, val regular: Boolean) {
    val any: Boolean get() = business || regular
}

/**
 * Opens a chat with the message PREFILLED using the official click-to-chat link (https://wa.me/...).
 * It never sends anything: the user presses Send inside WhatsApp. No accessibility services, no automation,
 * no WhatsApp Cloud API, no credentials.
 *
 * Package visibility: AndroidManifest declares <queries> for both packages, which is what lets
 * [availability] see them on Android 11+.
 */
@Singleton
class WhatsAppLauncher @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun availability() = WhatsAppAvailability(
        business = isInstalled(WhatsAppTarget.BUSINESS.pkg),
        regular = isInstalled(WhatsAppTarget.REGULAR.pkg),
    )

    private fun isInstalled(pkg: String): Boolean = try {
        val pm = ctx.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        else @Suppress("DEPRECATION") pm.getApplicationInfo(pkg, 0)
        info.enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** The preferred app: WhatsApp Business first, as requested; null if neither is installed. */
    fun preferred(): WhatsAppTarget? {
        val a = availability()
        return when {
            a.business -> WhatsAppTarget.BUSINESS
            a.regular -> WhatsAppTarget.REGULAR
            else -> null
        }
    }

    fun intentFor(phoneE164: String, message: String, target: WhatsAppTarget): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(WhatsAppLink.build(phoneE164, message)))
            .setPackage(target.pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** @return true if Android accepted the launch. Never throws. */
    fun launch(from: Context, phoneE164: String, message: String, target: WhatsAppTarget): Boolean = try {
        from.startActivity(intentFor(phoneE164, message, target))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
