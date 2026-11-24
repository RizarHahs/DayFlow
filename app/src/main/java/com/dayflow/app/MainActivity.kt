package com.dayflow.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.data.prefs.AppPrefs
import com.dayflow.app.domain.model.AppearanceSettings
import com.dayflow.app.domain.model.ThemeMode
import com.dayflow.app.platform.Actions
import com.dayflow.app.ui.DayflowRoot
import com.dayflow.app.ui.components.TimeFormat
import com.dayflow.app.ui.theme.DayflowTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: AppPrefs

    /** Screen requested by a notification (e.g. "review/20400"); consumed once by the root. */
    private var deepLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLink = intent?.getStringExtra(Actions.EXTRA_ROUTE)
        setContent {
            val appearance by prefs.appearance.collectAsStateWithLifecycle(initialValue = AppearanceSettings())
            val dark = when (appearance.theme) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            SideEffect { TimeFormat.is24h = appearance.use24h }
            // Transparent bars with icons that stay readable in either theme.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                onDispose { }
            }
            DayflowTheme(darkTheme = dark) {
                DayflowRoot(deepLink = deepLink, onDeepLinkHandled = { deepLink = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = intent.getStringExtra(Actions.EXTRA_ROUTE)
    }
}
