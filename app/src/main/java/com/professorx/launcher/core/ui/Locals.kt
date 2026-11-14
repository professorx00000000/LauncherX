package com.professorx.launcher.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.professorx.launcher.launcher.appdrawer.AppInfo

data class IconStyle(val shapeId: Int = 1, val showLabels: Boolean = true)

val LocalIconStyle = staticCompositionLocalOf { IconStyle() }
val LocalApps = staticCompositionLocalOf<Map<String, AppInfo>> { emptyMap() }
val LocalLaunch = staticCompositionLocalOf<(String) -> Unit> { {} }
