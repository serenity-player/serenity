package io.github.serenity_player.serenity.ui.theme

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Tints the wallpaper-based Material You scheme with the current album art's color. */
@Composable
fun ArtTheme(seed: Color?, content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val target = remember(seed, base, dark) { if (seed == null) base else base.tintedWith(seed, dark) }

    @Composable
    fun anim(c: Color): State<Color> = animateColorAsState(c, tween(700), label = "art")

    val scheme = base.copy(
        primary = anim(target.primary).value,
        onPrimary = anim(target.onPrimary).value,
        primaryContainer = anim(target.primaryContainer).value,
        onPrimaryContainer = anim(target.onPrimaryContainer).value,
        secondaryContainer = anim(target.secondaryContainer).value,
        onSecondaryContainer = anim(target.onSecondaryContainer).value,
        background = anim(target.background).value,
        surface = anim(target.surface).value,
    )
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes, content = content)
}

private fun ColorScheme.tintedWith(seed: Color, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(seed.toArgb(), it) }
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.35f, 0.85f)
    fun tone(s: Float, l: Float) = Color(ColorUtils.HSLToColor(floatArrayOf(hue, s.coerceIn(0f, 1f), l)))
    return if (dark) copy(
        primary = tone(sat, 0.78f), onPrimary = tone(sat, 0.15f),
        primaryContainer = tone(sat * 0.8f, 0.25f), onPrimaryContainer = tone(sat, 0.90f),
        secondaryContainer = tone(sat * 0.4f, 0.28f), onSecondaryContainer = tone(sat * 0.4f, 0.90f),
        background = tone(sat * 0.35f, 0.08f), surface = tone(sat * 0.35f, 0.08f),
    ) else copy(
        primary = tone(sat, 0.38f), onPrimary = Color.White,
        primaryContainer = tone(sat, 0.86f), onPrimaryContainer = tone(sat, 0.12f),
        secondaryContainer = tone(sat * 0.5f, 0.88f), onSecondaryContainer = tone(sat * 0.5f, 0.12f),
        background = tone(sat * 0.6f, 0.96f), surface = tone(sat * 0.6f, 0.96f),
    )
}

/** Dominant/vibrant color of the album art at [url]. Keeps the previous color while loading the next. */
@Composable
fun rememberArtColor(url: String?): Color? {
    val context = LocalContext.current
    return produceState<Color?>(initialValue = null, url) {
        value = if (url == null) null
        else withContext(Dispatchers.Default) { extractSeed(context, url) } ?: value
    }.value
}

private suspend fun extractSeed(context: Context, url: String): Color? {
    val request = ImageRequest.Builder(context).data(url).size(128).allowHardware(false).build()
    val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable ?: return null
    val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap()
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
    return swatch?.let { Color(it.rgb) }
}
