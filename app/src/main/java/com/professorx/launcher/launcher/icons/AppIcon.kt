package com.professorx.launcher.launcher.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.GlassIconContainer
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.ui.LocalIconStyle
import com.professorx.launcher.launcher.appdrawer.IconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val iconShapeNames = listOf("Circle", "Rounded", "Square", "Squircle")

fun iconShape(id: Int): Shape = when (id) {
    0 -> CircleShape
    1 -> RoundedCornerShape(percent = 26)
    2 -> RectangleShape
    else -> RoundedCornerShape(percent = 38)
}

@Composable
fun AppIcon(
    pkg: String,
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
    showLabel: Boolean = LocalIconStyle.current.showLabels,
) {
    val ctx = LocalContext.current
    val style = LocalIconStyle.current
    val theme = LocalTheme.current
    val icon by produceState<ImageBitmap?>(IconCache.peek(pkg), pkg) {
        if (value == null) value = withContext(Dispatchers.Default) { IconCache.get(ctx, pkg) }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GlassIconContainer(iconShape(style.shapeId), size) {
            val b = icon
            if (b != null) Image(b, contentDescription = label, modifier = Modifier.fillMaxSize().padding(size * 0.14f))
            else Box(Modifier)
        }
        if (showLabel) {
            Text(
                label,
                color = theme.text,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(size + 14.dp).padding(top = 3.dp),
            )
        }
    }
}
