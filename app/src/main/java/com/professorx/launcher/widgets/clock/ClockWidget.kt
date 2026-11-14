package com.professorx.launcher.widgets.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.ui.tickingState
import com.professorx.launcher.launcher.workspace.WorkItem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

/** payload = "digital|24" | "digital|12" | "analog|24" */
@Composable
fun ClockWidget(item: WorkItem) {
    val theme = LocalTheme.current
    val parts = item.payload.split("|")
    val analog = parts.getOrNull(0) == "analog"
    val is24 = parts.getOrNull(1) != "12"
    val now by tickingState(LocalDateTime.now(), 10_000L) { LocalDateTime.now() }

    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        if (analog) {
            Canvas(Modifier.size(minOf(maxWidth, maxHeight))) {
                val r = size.minDimension / 2f
                val c = center
                drawCircle(theme.glow.copy(alpha = 0.7f), r, c, style = Stroke(3f))
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0 - 90.0)
                    val s = Offset(c.x + (r * 0.86f * cos(a)).toFloat(), c.y + (r * 0.86f * sin(a)).toFloat())
                    val e = Offset(c.x + (r * 0.96f * cos(a)).toFloat(), c.y + (r * 0.96f * sin(a)).toFloat())
                    drawLine(Color.White.copy(alpha = 0.7f), s, e, 2f)
                }
                val hourDeg = (now.hour % 12) * 30f + now.minute * 0.5f
                hand(c, hourDeg, r * 0.5f, 6f, Color.White)
                hand(c, now.minute * 6f, r * 0.78f, 4f, theme.primary)
                drawCircle(theme.primary, 6f, c)
            }
        } else {
            val fs = minOf(maxHeight.value * 0.5f, maxWidth.value * 0.22f).coerceAtLeast(14f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    now.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a")),
                    color = theme.text,
                    fontSize = fs.sp,
                    style = TextStyle(shadow = Shadow(theme.glow, blurRadius = 24f)),
                )
                Text(
                    now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    color = theme.subText,
                    fontSize = (fs * 0.3f).coerceAtLeast(10f).sp,
                )
            }
        }
    }
}

private fun DrawScope.hand(center: Offset, deg: Float, len: Float, width: Float, color: Color) {
    val rad = Math.toRadians(deg - 90.0)
    drawLine(
        color, center,
        Offset(center.x + (len * cos(rad)).toFloat(), center.y + (len * sin(rad)).toFloat()),
        width, cap = StrokeCap.Round,
    )
}
