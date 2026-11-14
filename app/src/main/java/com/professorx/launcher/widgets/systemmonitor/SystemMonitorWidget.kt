package com.professorx.launcher.widgets.systemmonitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.ui.tickingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class SysInfo(
    val ramTotal: Long, val ramAvail: Long,
    val stTotal: Long, val stAvail: Long,
    val battPct: Int, val battTempC: Float?,
    val cores: Int, val soc: String,
)

object SysReader {
    fun read(ctx: Context): SysInfo {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        val sf = StatFs(Environment.getDataDirectory().path)
        val b: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val t = b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val soc = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else Build.HARDWARE
        return SysInfo(
            mi.totalMem, mi.availMem, sf.totalBytes, sf.availableBytes, pct,
            if (t == Int.MIN_VALUE) null else t / 10f,
            Runtime.getRuntime().availableProcessors(), soc ?: "",
        )
    }
}

private fun gb(b: Long) = String.format(Locale.US, "%.1f", b / 1073741824.0)

@Composable
fun SystemMonitorWidget() {
    val ctx = LocalContext.current
    val theme = LocalTheme.current
    val info by tickingState<SysInfo?>(null, 5_000L) { withContext(Dispatchers.IO) { SysReader.read(ctx) } }
    val i = info
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("SYSTEM", color = theme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (i == null) return@Column
        Row2("RAM  ${gb(i.ramTotal - i.ramAvail)}/${gb(i.ramTotal)} GB", (i.ramTotal - i.ramAvail).toFloat() / i.ramTotal)
        Row2("Storage  ${gb(i.stTotal - i.stAvail)}/${gb(i.stTotal)} GB", (i.stTotal - i.stAvail).toFloat() / i.stTotal)
        if (i.battPct >= 0) Row2("Battery  ${i.battPct}%" + (i.battTempC?.let { "  ·  ${String.format(Locale.US, "%.1f", it)}°C" } ?: ""), i.battPct / 100f)
        Text("${Build.MANUFACTURER} ${Build.MODEL}", color = theme.subText, fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
        Text("${i.cores} cores · ${i.soc}", color = theme.subText, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun Row2(label: String, progress: Float) {
    val theme = LocalTheme.current
    Text(label, color = theme.text, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        color = theme.primary,
        trackColor = theme.secondary.copy(alpha = 0.5f),
    )
}
