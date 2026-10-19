package bible.app.ui.art

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import bible.app.ui.theme.BibleColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Наибольшая степень двойки, при которой картинка ещё не меньше места на экране. */
fun sampleSizeFor(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
    if (targetWidth <= 0 || targetHeight <= 0) return 1
    var sample = 1
    while (width / (sample * 2) >= targetWidth && height / (sample * 2) >= targetHeight) sample *= 2
    return sample
}

/** Декодированные арты: ключ — ресурс и коэффициент уменьшения. До 1/8 памяти приложения. */
private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

private fun key(res: Int, w: Int, h: Int) = "$res@${w}x$h"

private fun decode(resources: Resources, res: Int, targetWidth: Int, targetHeight: Int): ImageBitmap {
    cache.get(key(res, targetWidth, targetHeight))?.let { return it }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(resources, res, bounds)
    val sample = sampleSizeFor(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight)
    val bitmap = BitmapFactory.decodeResource(resources, res, BitmapFactory.Options().apply { inSampleSize = sample })
    return bitmap.asImageBitmap().also {
        bitmap.prepareToDraw()
        cache.put(key(res, targetWidth, targetHeight), it)
    }
}

/**
 * Арт, декодированный в фоне под размер на экране: прокрутка и листание не ждут декодирования.
 * Пока картинка грузится, виден спокойный фон; потом она мягко проявляется.
 */
@Composable
fun ArtImage(@DrawableRes res: Int, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val focus = remember(res) { ART_FOCUS_Y[runCatching { resources.getResourceEntryName(res) }.getOrNull()] ?: 0.5f }
    BoxWithConstraints(modifier.background(BibleColors.Page)) {
        val w = constraints.maxWidth.takeIf { constraints.hasBoundedWidth } ?: 0
        val h = constraints.maxHeight.takeIf { constraints.hasBoundedHeight } ?: 0
        var bitmap by remember(res, w, h) { mutableStateOf(cache.get(key(res, w, h))) }
        LaunchedEffect(res, w, h) {
            if (bitmap != null) return@LaunchedEffect
            bitmap = withContext(Dispatchers.Default) { decode(resources, res, w, h) }
        }
        val alpha by animateFloatAsState(if (bitmap != null) 1f else 0f, tween(180), label = "art")
        bitmap?.let {
            Image(
                it,
                null,
                Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
                alignment = FocusAlignment(focus),
                contentScale = ContentScale.Crop,
            )
        } ?: Box(Modifier.fillMaxSize())
    }
}
