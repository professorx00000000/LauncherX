package com.professorx.launcher.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.LocalTheme
import com.professorx.launcher.core.storage.decodeSampled
import com.professorx.launcher.launcher.workspace.WorkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Photo is center-cropped to the item's width/height (resize = recrop). */
@Composable
fun PhotoWidget(item: WorkItem) {
    val theme = LocalTheme.current
    val bmp by produceState<ImageBitmap?>(null, item.payload) {
        value = withContext(Dispatchers.IO) { decodeSampled(item.payload, 900) }
    }
    val shape = RoundedCornerShape(item.corner.dp)
    val b = bmp
    Box(
        Modifier
            .fillMaxSize()
            .shadow(8.dp, shape, ambientColor = theme.glow, spotColor = theme.glow)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.35f))
            .then(if (item.border) Modifier.border(2.dp, theme.primary, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (b != null) {
            Image(b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("Image unavailable", color = theme.subText, fontSize = 11.sp)
        }
    }
}
