package com.professorx.launcher.launcher.workspace

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.ui.LocalApps
import com.professorx.launcher.core.ui.LocalLaunch
import com.professorx.launcher.launcher.LauncherViewModel
import com.professorx.launcher.launcher.folders.GlassFolder
import com.professorx.launcher.launcher.icons.AppIcon
import com.professorx.launcher.widgets.GlassWidget
import com.professorx.launcher.widgets.PhotoWidget
import com.professorx.launcher.widgets.calendar.CalendarWidget
import com.professorx.launcher.widgets.clock.ClockWidget
import com.professorx.launcher.widgets.screentime.ScreenTimeWidget
import com.professorx.launcher.widgets.systemmonitor.SystemMonitorWidget
import kotlin.math.roundToInt

private const val GRID = 8f
private const val MIN_SIZE = 48f

@Composable
fun WorkspacePage(
    page: Int,
    state: LauncherState,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onOpenDrawer: (Boolean) -> Unit,
    onLongPressEmpty: () -> Unit,
    vm: LauncherViewModel,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = maxWidth.value
        val maxH = maxHeight.value
        // Background: tap = deselect, long-press = add menu, swipe up = drawer, swipe down = drawer+search
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onSelect(null) }, onLongPress = { onLongPressEmpty() })
                }
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (total < -150f) onOpenDrawer(false) else if (total > 150f) onOpenDrawer(true)
                        },
                        onVerticalDrag = { _, dy -> total += dy },
                    )
                },
        )
        state.items.filter { it.page == page }.forEach { item ->
            key(item.id) {
                WorkItemView(
                    item = item,
                    maxW = maxW,
                    maxH = maxH,
                    selected = item.id == selectedId,
                    canEdit = !state.settings.layoutLocked && !item.locked,
                    snap = state.settings.snap,
                    onSelect = onSelect,
                    vm = vm,
                )
            }
        }
    }
}

@Composable
fun WorkItemView(
    item: WorkItem,
    maxW: Float,
    maxH: Float,
    selected: Boolean,
    canEdit: Boolean,
    snap: Boolean,
    onSelect: (String?) -> Unit,
    vm: LauncherViewModel,
) {
    val density = LocalDensity.current.density
    val launch = LocalLaunch.current
    val theme = LocalTheme.current
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    var dw by remember { mutableFloatStateOf(0f) }
    var dh by remember { mutableFloatStateOf(0f) }

    val w = (item.w + dw).coerceIn(MIN_SIZE, maxOf(MIN_SIZE, maxW))
    val h = (item.h + dh).coerceIn(MIN_SIZE, maxOf(MIN_SIZE, maxH))
    val x = (item.x + dx).coerceIn(0f, (maxW - w).coerceAtLeast(0f))
    val y = (item.y + dy).coerceIn(0f, (maxH - h).coerceAtLeast(0f))

    fun sv(v: Float) = if (snap) (v / GRID).roundToInt() * GRID else v

    // Always the latest values (the gesture lambdas below are long-lived).
    val commit = rememberUpdatedState {
        val nx = sv(x).coerceIn(0f, (maxW - w).coerceAtLeast(0f))
        val ny = sv(y).coerceIn(0f, (maxH - h).coerceAtLeast(0f))
        val nw = sv(w).coerceIn(MIN_SIZE, maxOf(MIN_SIZE, maxW - nx))
        val nh = sv(h).coerceIn(MIN_SIZE, maxOf(MIN_SIZE, maxH - ny))
        vm.updateItem(item.id) { it.copy(x = nx, y = ny, w = nw, h = nh) }
        dx = 0f; dy = 0f; dw = 0f; dh = 0f
    }
    val onMove = rememberUpdatedState<(Float, Float) -> Unit> { ddx, ddy ->
        dx = (item.x + dx + ddx / density).coerceIn(0f, (maxW - w).coerceAtLeast(0f)) - item.x
        dy = (item.y + dy + ddy / density).coerceIn(0f, (maxH - h).coerceAtLeast(0f)) - item.y
    }

    Box(
        Modifier
            .offset { IntOffset((x * density).roundToInt(), (y * density).roundToInt()) }
            .size(w.dp, h.dp)
            .zIndex(if (selected) 1f else 0f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = item.rotation; alpha = item.alpha }
                .pointerInput(item.id, selected) {
                    detectTapGestures(
                        onTap = {
                            if (selected) onSelect(null)
                            else if (item.type == ItemType.APP) launch(item.payload)
                        },
                        onLongPress = { onSelect(item.id) },
                    )
                }
                .then(
                    if (selected && canEdit) {
                        Modifier.pointerInput(item.id, "move") {
                            detectDragGestures(
                                onDrag = { change, d ->
                                    change.consume()
                                    onMove.value(d.x, d.y)
                                },
                                onDragEnd = { commit.value() },
                                onDragCancel = { dx = 0f; dy = 0f },
                            )
                        }
                    } else Modifier,
                ),
        ) {
            when (item.type) {
                ItemType.CLOCK -> GlassWidget(item) { ClockWidget(item) }
                ItemType.SYSTEM -> GlassWidget(item) { SystemMonitorWidget() }
                ItemType.SCREENTIME -> GlassWidget(item) { ScreenTimeWidget() }
                ItemType.CALENDAR -> GlassWidget(item) { CalendarWidget() }
                ItemType.PHOTO -> PhotoWidget(item)
                ItemType.FOLDER -> GlassWidget(item) { GlassFolder(item) }
                ItemType.APP -> AppTile(item)
            }
        }
        if (selected) {
            Box(Modifier.fillMaxSize().border(1.5.dp, theme.primary, RoundedCornerShape(item.corner.dp)))
        }
        if (selected && canEdit) {
            ResizeHandle(
                Modifier.align(Alignment.CenterEnd), theme.primary,
                onDrag = { ddx, _ -> dw = (item.w + dw + ddx / density).coerceIn(MIN_SIZE, maxOf(MIN_SIZE, maxW - x)) - item.w },
                onEnd = { commit.value() },
            )
            ResizeHandle(
                Modifier.align(Alignment.BottomCenter), theme.primary,
                onDrag = { _, ddy -> dh = (item.h + dh + ddy / density).coerceIn(MIN_SIZE, maxOf(MIN_SIZE, maxH - y)) - item.h },
                onEnd = { commit.value() },
            )
            ResizeHandle(
                Modifier.align(Alignment.BottomEnd), theme.primary,
                onDrag = { ddx, ddy ->
                    dw = (item.w + dw + ddx / density).coerceIn(MIN_SIZE, maxOf(MIN_SIZE, maxW - x)) - item.w
                    dh = (item.h + dh + ddy / density).coerceIn(MIN_SIZE, maxOf(MIN_SIZE, maxH - y)) - item.h
                },
                onEnd = { commit.value() },
            )
        }
    }
}

@Composable
private fun ResizeHandle(
    modifier: Modifier,
    color: Color,
    onDrag: (Float, Float) -> Unit,
    onEnd: () -> Unit,
) {
    val od = rememberUpdatedState(onDrag)
    val oe = rememberUpdatedState(onEnd)
    Box(
        modifier.size(36.dp).pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, d ->
                    change.consume()
                    od.value(d.x, d.y)
                },
                onDragEnd = { oe.value() },
                onDragCancel = { oe.value() },
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color).border(1.dp, Color.White, CircleShape))
    }
}

@Composable
private fun AppTile(item: WorkItem) {
    val a = LocalApps.current[item.payload]
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (a != null) {
            val s = minOf(maxWidth, maxHeight - 18.dp).coerceAtLeast(24.dp)
            AppIcon(a.pkg, a.label, s)
        } else {
            Text("Missing app", color = Color.Gray, fontSize = 10.sp)
        }
    }
}
