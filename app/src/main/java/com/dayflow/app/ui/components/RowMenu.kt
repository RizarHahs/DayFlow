package com.dayflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayflow.app.ui.theme.Df
import com.dayflow.app.ui.theme.DfType
import com.dayflow.app.ui.theme.PriorityColors

data class MenuAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** Overflow button with a dropdown of row actions (48dp target). */
@Composable
fun RowMenu(actions: List<MenuAction>, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(44.dp).pressable(haptic = null) { open = true }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MoreVert, "More", tint = Df.colors.textSecondary, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(Df.colors.raised),
        ) {
            actions.forEach { a ->
                DropdownMenuItem(
                    text = {
                        Text(
                            a.label, style = DfType.body,
                            color = if (a.destructive) PriorityColors.Critical else Df.colors.textPrimary,
                        )
                    },
                    onClick = { open = false; a.onClick() },
                )
            }
        }
    }
}
