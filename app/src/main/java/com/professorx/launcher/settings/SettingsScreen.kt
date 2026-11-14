@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.professorx.launcher.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.GlassButton
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.design.Themes
import com.professorx.launcher.core.ui.blockTouches
import com.professorx.launcher.launcher.LauncherViewModel
import com.professorx.launcher.launcher.icons.iconShapeNames
import com.professorx.launcher.launcher.workspace.LabeledSlider
import com.professorx.launcher.launcher.workspace.LauncherState

@Composable
fun SettingsScreen(
    vm: LauncherViewModel,
    state: LauncherState,
    onPickWallpaper: () -> Unit,
    onClose: () -> Unit,
) {
    val theme = LocalTheme.current
    val ctx = LocalContext.current
    val s = state.settings

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
            }.isSuccess
            Toast.makeText(ctx, if (ok) "Backup saved" else "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            val ok = text != null && vm.importJson(text)
            Toast.makeText(ctx, if (ok) "Layout restored (photos are not included in backups)" else "Not a valid backup", Toast.LENGTH_LONG).show()
        }
    }

    // Top 30% stays transparent so changes are visible live behind the sheet.
    Column(Modifier.fillMaxSize()) {
        Spacer(
            Modifier.weight(0.3f).fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
        )
        Column(
            Modifier
                .weight(0.7f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xF2080808))
                .blockTouches()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", color = theme.text, fontSize = 20.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
                GlassButton("Close") { onClose() }
            }

            Header("Themes")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Themes.all.forEachIndexed { i, t -> GlassButton(t.name, selected = s.themeId == i) { vm.setSettings { it.copy(themeId = i) } } }
            }
            LabeledSlider("Glass opacity %", s.glassAlpha * 100f, 5f..50f) { v -> vm.setSettings { it.copy(glassAlpha = v / 100f) } }

            Header("Wallpaper")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("Choose image") { onPickWallpaper() }
                GlassButton("Default") { vm.clearWallpaper() }
            }
            LabeledSlider("Dim %", s.dim * 100f, 0f..80f) { v -> vm.setSettings { it.copy(dim = v / 100f) } }

            Header("Home screen")
            SwitchRow("Snap to grid", s.snap) { b -> vm.setSettings { it.copy(snap = b) } }
            SwitchRow("Lock layout", s.layoutLocked) { b -> vm.setSettings { it.copy(layoutLocked = b) } }
            SwitchRow("Show dock", s.showDock) { b -> vm.setSettings { it.copy(showDock = b) } }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pages: ${s.pages}", color = theme.text, modifier = Modifier.weight(1f))
                GlassButton("Add page") { vm.addPage() }
                GlassButton("Remove last") {
                    if (!vm.removeLastPage()) Toast.makeText(ctx, "Last page must be empty (and at least 1 page stays)", Toast.LENGTH_SHORT).show()
                }
            }

            Header("Icons")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                iconShapeNames.forEachIndexed { i, n -> GlassButton(n, selected = s.iconShape == i) { vm.setSettings { it.copy(iconShape = i) } } }
            }
            SwitchRow("Show app labels", s.showLabels) { b -> vm.setSettings { it.copy(showLabels = b) } }

            Header("App drawer")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 4, 5, 6).forEach { c -> GlassButton("$c cols", selected = s.drawerColumns == c) { vm.setSettings { it.copy(drawerColumns = c) } } }
            }
            GlassButton("Unhide all apps (${state.hidden.size} hidden)") { vm.unhideAll() }

            Header("Backup & restore")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("Export") { export.launch("professorx-backup.json") }
                GlassButton("Restore") { import.launch(arrayOf("*/*")) }
            }

            Header("About")
            Text(
                "ProfessorX Launcher · Red Phantom X · v1.0.0\nHACK THE SYSTEM. CUSTOMIZE YOUR WORLD. STAY ANONYMOUS.\nAll data stays on this device.",
                color = theme.subText, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun Header(t: String) {
    Text(t.uppercase(), color = LocalTheme.current.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val theme = LocalTheme.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = theme.text, modifier = Modifier.weight(1f))
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = theme.primary))
    }
}
