package com.professorx.launcher.core.storage

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlin.math.max

/** Decodes a local image downsampled so the longest side is about [maxDim] px (keeps memory low). */
fun decodeSampled(path: String, maxDim: Int): ImageBitmap? {
    if (path.isBlank()) return null
    val f = File(path)
    if (!f.exists()) return null
    return runCatching {
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(f)) { decoder, info, _ ->
                val m = max(info.size.width, info.size.height)
                decoder.setTargetSampleSize(max(1, m / maxDim))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }.asImageBitmap()
        } else {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, o)
            var s = 1
            while (max(o.outWidth, o.outHeight) / s > maxDim) s *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
        }
    }.getOrNull()
}
