package com.dayflow.app.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dayflow.app.platform.setup.SetupIntents
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfType
import com.dayflow.app.ui.theme.PriorityColors

/** In-context permission nudge: only appears while notifications are actually off. */
@Composable
fun NotificationHint(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        enabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }
    if (enabled) return
    val c = PriorityColors.High
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.14f)).padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Notifications are off, so alarms and timers can't alert you.",
            style = DfType.bodySmall, color = Df.colors.textPrimary, modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        Text(
            if (Build.VERSION.SDK_INT >= 33 && !asked) "Allow" else "Settings",
            style = DfType.bodyMedium, color = c,
            modifier = Modifier.heightIn(min = 44.dp).pressable {
                if (Build.VERSION.SDK_INT >= 33 && !asked) { asked = true; launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                else SetupIntents.launch(ctx, SetupIntents.appNotificationSettings(ctx))
            }.padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}
