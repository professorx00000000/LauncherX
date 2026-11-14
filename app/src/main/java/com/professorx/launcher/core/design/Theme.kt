package com.professorx.launcher.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class LauncherTheme(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val glow: Color,
    val text: Color = Color.White,
    val subText: Color = Color(0xFFB8B8C2),
)

object Themes {
    val all = listOf(
        LauncherTheme("Red Phantom", Color(0xFFFF003C), Color(0xFF650018), Color(0xFF050505), Color(0xFF101014), Color(0xFFFF1744)),
        LauncherTheme("Emerald Hacker", Color(0xFF00E676), Color(0xFF004D2A), Color(0xFF040A07), Color(0xFF0C1410), Color(0xFF00E676)),
        LauncherTheme("Purple Phantom", Color(0xFFB000FF), Color(0xFF3A0066), Color(0xFF07040A), Color(0xFF120C18), Color(0xFFD050FF)),
        LauncherTheme("Matrix Green", Color(0xFF39FF14), Color(0xFF0B3D0B), Color(0xFF000000), Color(0xFF071007), Color(0xFF39FF14)),
        LauncherTheme("Midnight Black", Color(0xFF8899FF), Color(0xFF1A1A2E), Color(0xFF000000), Color(0xFF0B0B10), Color(0xFF8899FF)),
        LauncherTheme("Arctic Glass", Color(0xFF4FC3F7), Color(0xFF0D3B5A), Color(0xFF0A1118), Color(0xFF111A22), Color(0xFF81D4FA)),
    )
}

val LocalTheme = staticCompositionLocalOf { Themes.all[0] }
val LocalGlassAlpha = staticCompositionLocalOf { 0.18f }

@Composable
fun ProfessorXTheme(theme: LauncherTheme, glassAlpha: Float, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = theme.primary,
        secondary = theme.secondary,
        background = theme.background,
        surface = theme.surface,
        onPrimary = Color.White,
        onBackground = theme.text,
        onSurface = theme.text,
        onSurfaceVariant = theme.subText,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalTheme provides theme,
            LocalGlassAlpha provides glassAlpha,
            content = content,
        )
    }
}
