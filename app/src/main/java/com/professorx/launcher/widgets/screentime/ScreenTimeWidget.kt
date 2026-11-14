package com.professorx.launcher.widgets.screentime

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.app.usage.UsageStatsManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.GlassButton
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.ui.tickingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

sealed interface UsageState {
    data object Loading : UsageState
    data object NoAccess : UsageState
    data class Data(
        val today: Long, val week: Long,
        val perDay: List<Long>, val top: List<Pair<String, Long>>,
    ) : UsageState
}

object UsageRepo {
    @Suppress("DEPRECATION")
    fun hasAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Sum of app foreground time per day (excluding this launcher), as reported by UsageStatsManager. */
    fun read(ctx: Context): UsageState {
        if (!hasAccess(ctx)) return UsageState.NoAccess
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return UsageState.NoAccess
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val perDay = ArrayList<Long>()
        var top = emptyList<Pair<String, Long>>()
        for (i in 6 downTo 0) {
            val start = LocalDate.now().minusDays(i.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = minOf(start + 86_400_000L, now)
            val entries = usm.queryAndAggregateUsageStats(start, end).values
                .filter { it.packageName != ctx.packageName && it.totalTimeInForeground > 0 }
            perDay.add(entries.sumOf { it.totalTimeInForeground })
            if (i == 0) {
                top = entries.sortedByDescending { it.totalTimeInForeground }
                    .take(3).map { it.packageName to it.totalTimeInForeground }
            }
        }
        return UsageState.Data(perDay.last(), perDay.sum(), perDay, top)
    }
}

private fun dur(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms / 60_000) % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun ScreenTimeWidget() {
    val ctx = LocalContext.current
    val theme = LocalTheme.current
    val state by tickingState<UsageState>(UsageState.Loading, 60_000L) {
        withContext(Dispatchers.IO) { UsageRepo.read(ctx) }
    }
    var ask by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("SCREEN TIME", color = theme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        when (val s = state) {
            UsageState.Loading -> Text("…", color = theme.subText)
            UsageState.NoAccess -> {
                Text(
                    "Usage access is needed to show app usage.",
                    color = theme.subText, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
                GlassButton("Grant access") { ask = true }
            }
            is UsageState.Data -> BoxWithConstraints(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Text(dur(s.today), color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.Light)
                    Text("Today · week ${dur(s.week)}", color = theme.subText, fontSize = 10.sp)
                    Canvas(Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp)) {
                        val n = s.perDay.size
                        val mx = (s.perDay.maxOrNull() ?: 1L).coerceAtLeast(1L)
                        val slot = size.width / n
                        val bw = slot * 0.6f
                        s.perDay.forEachIndexed { idx, v ->
                            val bh = size.height * (v.toFloat() / mx)
                            drawRoundRect(
                                color = if (idx == n - 1) theme.primary else theme.primary.copy(alpha = 0.4f),
                                topLeft = Offset(idx * slot + (slot - bw) / 2f, size.height - bh),
                                size = Size(bw, bh),
                                cornerRadius = CornerRadius(4f, 4f),
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        for (i in 6 downTo 0) {
                            Text(
                                LocalDate.now().minusDays(i.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                color = theme.subText, fontSize = 9.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (maxHeight > 190.dp) {
                        val pm = ctx.packageManager
                        s.top.forEach { (pkg, ms) ->
                            val name = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                            Text("$name  ${dur(ms)}", color = theme.text, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (ask) {
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text("Usage access") },
            text = {
                Text(
                    "Android only shares per-app usage time with apps you allow. On the next screen, " +
                        "choose ProfessorX Launcher and turn on “Permit usage access”. " +
                        "Usage data stays on this device.",
                )
            },
            confirmButton = {
                TextButton({
                    ask = false
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                }) { Text("Open settings") }
            },
            dismissButton = { TextButton({ ask = false }) { Text("Not now") } },
        )
    }
}
