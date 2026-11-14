package com.professorx.launcher.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Liquid-glass surface: dark tint + white translucent gradient + thin gradient border + neon glow.
 * Note: this is a translucent-surface approximation. True backdrop blur of the wallpaper is not
 * implemented (Compose Modifier.blur blurs a layer's own content, not what is behind it).
 */
fun Modifier.glass(
    shape: Shape,
    alpha: Float,
    glow: Color,
    elevation: Dp = 8.dp,
): Modifier = this
    .shadow(elevation, shape, ambientColor = glow, spotColor = glow)
    .clip(shape)
    .background(Color.Black.copy(alpha = 0.30f))
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = (alpha * 1.8f).coerceAtMost(0.9f)),
                Color.White.copy(alpha = alpha * 0.6f),
            ),
        ),
    )
    .border(
        1.dp,
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.35f), glow.copy(alpha = 0.45f), Color.White.copy(alpha = 0.10f)),
        ),
        shape,
    )

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = LocalTheme.current
    Box(modifier.glass(RoundedCornerShape(corner), LocalGlassAlpha.current, t.glow), content = content)
}

@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val t = LocalTheme.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .glass(shape, LocalGlassAlpha.current, t.glow, elevation = 4.dp)
            .background(if (selected) t.primary.copy(alpha = 0.40f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = t.text, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
fun GlassSearchBar(hint: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = LocalTheme.current
    GlassCard(modifier.clickable(onClick = onClick), corner = 26.dp) {
        Text(
            hint,
            color = t.subText,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
fun GlassDock(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val t = LocalTheme.current
    Row(
        modifier.glass(RoundedCornerShape(32.dp), LocalGlassAlpha.current, t.glow, elevation = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun GlassIconContainer(
    shape: Shape,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = LocalTheme.current
    Box(
        modifier.size(size).glass(shape, LocalGlassAlpha.current, t.glow, elevation = 4.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
