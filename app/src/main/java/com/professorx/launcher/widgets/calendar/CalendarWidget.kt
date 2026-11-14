package com.professorx.launcher.widgets.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.LocalTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Manual monthly calendar - needs no permission. (Event integration is not implemented yet.) */
@Composable
fun CalendarWidget() {
    val theme = LocalTheme.current
    var ym by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val offset = (ym.atDay(1).dayOfWeek.value - first.value + 7) % 7
    val len = ym.lengthOfMonth()
    val rows = (offset + len + 6) / 7

    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = theme.primary, fontSize = 22.sp, modifier = Modifier.clickable { ym = ym.minusMonths(1) }.padding(horizontal = 12.dp))
            Text(
                ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.year,
                color = theme.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            Text("›", color = theme.primary, fontSize = 22.sp, modifier = Modifier.clickable { ym = ym.plusMonths(1) }.padding(horizontal = 12.dp))
        }
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                Text(
                    first.plus(i.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    color = theme.subText, fontSize = 10.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth()) {
            for (r in 0 until rows) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val day = r * 7 + c - offset + 1
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            if (day in 1..len) {
                                val isToday = ym.atDay(day) == today
                                Box(
                                    Modifier
                                        .background(if (isToday) theme.primary else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                ) {
                                    Text(day.toString(), color = theme.text, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
