package com.professorx.launcher.core.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

fun Context.findActivity(): ComponentActivity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is ComponentActivity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Polls [read] every [periodMs] ONLY while the activity is STARTED (battery-friendly:
 * nothing runs while the launcher is in the background).
 */
@Composable
fun <T> tickingState(initial: T, periodMs: Long, read: suspend () -> T): State<T> {
    val lifecycle = LocalContext.current.findActivity()?.lifecycle
    val readState = rememberUpdatedState(read)
    return produceState(initial, lifecycle) {
        if (lifecycle == null) {
            while (true) {
                val r = readState.value
                value = r()
                delay(periodMs)
            }
        } else {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val r = readState.value
                    value = r()
                    delay(periodMs)
                }
            }
        }
    }
}

/** Swallows leftover touches so overlays don't leak gestures to the home screen below. */
fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}
