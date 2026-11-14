@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.professorx.launcher.launcher.appdrawer

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.GlassButton
import com.professorx.launcher.core.design.GlassCard
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.ui.blockTouches
import com.professorx.launcher.launcher.icons.AppIcon
import com.professorx.launcher.launcher.workspace.LauncherState
import kotlinx.coroutines.delay

@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    state: LauncherState,
    focusSearch: Boolean,
    onLaunch: (String) -> Unit,
    onAddHome: (String) -> Unit,
    onAddDock: (String) -> Unit,
    onFav: (String) -> Unit,
    onHide: (String) -> Unit,
    onClose: () -> Unit,
) {
    val theme = LocalTheme.current
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            delay(300)
            runCatching { focus.requestFocus() }
        }
    }
    val list = remember(apps, state.hidden, state.favorites, state.recents, query, tab) {
        val visible = apps.filter { it.pkg !in state.hidden }
        val byTab = when (tab) {
            1 -> visible.filter { it.pkg in state.favorites }
            2 -> state.recents.mapNotNull { r -> visible.firstOrNull { it.pkg == r } }
            else -> visible
        }
        if (query.isBlank()) byTab else byTab.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.background.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.98f))))
            .blockTouches()
            .systemBarsPadding()
            .padding(horizontal = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Apps", color = theme.text, fontSize = 22.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
            GlassButton("Close") { onClose() }
        }
        GlassCard(Modifier.fillMaxWidth().padding(vertical = 10.dp), corner = 24.dp) {
            Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = theme.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(theme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { list.firstOrNull()?.let { onLaunch(it.pkg) } }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search apps", color = theme.subText, fontSize = 16.sp)
                        inner()
                    },
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Favorites", "Recent").forEachIndexed { i, t ->
                GlassButton(t, selected = tab == i) { tab = i }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(state.settings.drawerColumns),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(list, key = { it.pkg }) { app ->
                var menu by remember { mutableStateOf(false) }
                Box(
                    Modifier.combinedClickable(onClick = { onLaunch(app.pkg) }, onLongClick = { menu = true }),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    AppIcon(app.pkg, app.label, 56.dp)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Add to Home") }, onClick = { menu = false; onAddHome(app.pkg) })
                        DropdownMenuItem(text = { Text("Add to Dock") }, onClick = { menu = false; onAddDock(app.pkg) })
                        DropdownMenuItem(
                            text = { Text(if (app.pkg in state.favorites) "Remove favorite" else "Favorite") },
                            onClick = { menu = false; onFav(app.pkg) },
                        )
                        DropdownMenuItem(text = { Text("Hide from drawer") }, onClick = { menu = false; onHide(app.pkg) })
                    }
                }
            }
        }
    }
}
