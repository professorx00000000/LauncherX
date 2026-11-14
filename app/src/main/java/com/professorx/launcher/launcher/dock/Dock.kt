@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.professorx.launcher.launcher.dock

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.GlassDock
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.ui.LocalApps
import com.professorx.launcher.core.ui.LocalLaunch
import com.professorx.launcher.launcher.icons.AppIcon

@Composable
fun Dock(dock: List<String>, onRemove: (String) -> Unit) {
    val apps = LocalApps.current
    val launch = LocalLaunch.current
    val theme = LocalTheme.current
    val present = dock.filter { apps.containsKey(it) }
    GlassDock(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(78.dp)) {
        if (present.isEmpty()) {
            Text("Long-press an app in the drawer to add it here", color = theme.subText, fontSize = 12.sp)
        }
        present.forEach { pkg ->
            var menu by remember { mutableStateOf(false) }
            Box(Modifier.combinedClickable(onClick = { launch(pkg) }, onLongClick = { menu = true })) {
                AppIcon(pkg, apps[pkg]?.label ?: pkg, 52.dp, showLabel = false)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Remove from dock") }, onClick = { menu = false; onRemove(pkg) })
                }
            }
        }
    }
}
