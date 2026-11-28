package com.dayflow.app.ui.nav

import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import com.dayflow.app.ui.components.glass
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfType

/** Floating glass-style bar: content scrolls beneath it. */
@Composable
fun DayflowBottomBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .glass(shape, raised = true, chrome = true)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            val indicator by animateColorAsState(
                if (selected) Df.colors.textPrimary.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent,
                tween(220), label = "tabBg",
            )
            val tint by animateColorAsState(
                if (selected) Df.colors.textPrimary else Df.colors.textTertiary, tween(220), label = "tabTint",
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { this.selected = selected }
                    .pressable(pressedScale = 0.94f, role = Role.Tab) { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(width = 54.dp, height = 30.dp).clip(RoundedCornerShape(50)).background(indicator),
                    contentAlignment = Alignment.Center,
                ) { Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.height(2.dp))
                Text(tab.label, style = DfType.label.copy(fontSize = 10.sp), color = tint)
            }
        }
    }
}
