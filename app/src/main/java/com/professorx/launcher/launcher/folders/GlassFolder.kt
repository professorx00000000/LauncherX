package com.professorx.launcher.launcher.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.ui.LocalApps
import com.professorx.launcher.core.ui.LocalLaunch
import com.professorx.launcher.launcher.icons.AppIcon
import com.professorx.launcher.launcher.workspace.WorkItem

/** Folder contents: icons re-flow automatically to whatever width/height the folder has. */
@Composable
fun GlassFolder(item: WorkItem) {
    val theme = LocalTheme.current
    val apps = LocalApps.current
    val launch = LocalLaunch.current
    val present = item.apps.mapNotNull { apps[it] }
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Text(item.payload, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (present.isEmpty()) {
            Text("Empty. Long-press, then Edit to add apps.", color = theme.subText, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        } else {
            LazyVerticalGrid(GridCells.Adaptive(60.dp), Modifier.fillMaxSize().padding(top = 6.dp)) {
                items(present, key = { it.pkg }) { a ->
                    AppIcon(a.pkg, a.label, 44.dp, Modifier.clickable { launch(a.pkg) }.padding(2.dp))
                }
            }
        }
    }
}
