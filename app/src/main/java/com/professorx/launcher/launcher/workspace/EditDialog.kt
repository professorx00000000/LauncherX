@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.professorx.launcher.launcher.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.GlassButton
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.launcher.LauncherViewModel
import com.professorx.launcher.launcher.appdrawer.AppInfo

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val theme = LocalTheme.current
    Column(Modifier.fillMaxWidth()) {
        Text("$label: ${value.toInt()}", fontSize = 12.sp, color = theme.subText)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = theme.primary, activeTrackColor = theme.primary),
        )
    }
}

@Composable
fun EditDialog(
    item: WorkItem,
    maxW: Float,
    maxH: Float,
    apps: List<AppInfo>,
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    val theme = LocalTheme.current
    val id = item.id
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onDismiss) { Text("Done") } },
        title = { Text("Edit") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LabeledSlider("Opacity %", item.alpha * 100f, 20f..100f) { v -> vm.updateItem(id) { it.copy(alpha = v / 100f) } }
                LabeledSlider("Corner radius", item.corner, 0f..60f) { v -> vm.updateItem(id) { it.copy(corner = v) } }
                LabeledSlider("Width", item.w, 48f..maxOf(60f, maxW)) { v -> vm.updateItem(id) { it.copy(w = v) } }
                LabeledSlider("Height", item.h, 48f..maxOf(60f, maxH)) { v -> vm.updateItem(id) { it.copy(h = v) } }
                LabeledSlider("Rotation", item.rotation, -180f..180f) { v -> vm.updateItem(id) { it.copy(rotation = v) } }

                when (item.type) {
                    ItemType.CLOCK -> {
                        val parts = item.payload.split("|")
                        val analog = parts.getOrNull(0) == "analog"
                        val h12 = parts.getOrNull(1) == "12"
                        fun set(a: Boolean, t12: Boolean) =
                            vm.updateItem(id) { it.copy(payload = (if (a) "analog" else "digital") + "|" + (if (t12) "12" else "24")) }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            GlassButton("Digital", selected = !analog) { set(false, h12) }
                            GlassButton("Analog", selected = analog) { set(true, h12) }
                            GlassButton("24h", selected = !h12) { set(analog, false) }
                            GlassButton("12h", selected = h12) { set(analog, true) }
                        }
                    }
                    ItemType.PHOTO -> {
                        Text("Size", fontSize = 12.sp, color = theme.subText, modifier = Modifier.padding(top = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("XS" to 72f, "S" to 120f, "M" to 180f, "L" to 280f).forEach { (n, s) ->
                                GlassButton(n) { vm.updateItem(id) { it.copy(w = s, h = s) } }
                            }
                        }
                        Text("Aspect ratio (keeps width)", fontSize = 12.sp, color = theme.subText, modifier = Modifier.padding(top = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1:1" to 1f, "4:3" to 4f / 3f, "3:4" to 3f / 4f, "16:9" to 16f / 9f).forEach { (n, r) ->
                                GlassButton(n) { vm.updateItem(id) { it.copy(h = (it.w / r).coerceAtLeast(48f)) } }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Neon border", Modifier.weight(1f))
                            Switch(item.border, { b -> vm.updateItem(id) { it.copy(border = b) } })
                        }
                    }
                    ItemType.FOLDER -> {
                        var name by remember { mutableStateOf(item.payload) }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; vm.updateItem(id) { f -> f.copy(payload = it) } },
                            label = { Text("Folder name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        Text("Apps in folder", fontSize = 12.sp, color = theme.subText, modifier = Modifier.padding(top = 8.dp))
                        apps.forEach { a ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = a.pkg in item.apps,
                                    onCheckedChange = { on ->
                                        vm.updateItem(id) { f -> f.copy(apps = if (on) f.apps + a.pkg else f.apps - a.pkg) }
                                    },
                                )
                                Text(a.label, fontSize = 14.sp, maxLines = 1)
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
    )
}
