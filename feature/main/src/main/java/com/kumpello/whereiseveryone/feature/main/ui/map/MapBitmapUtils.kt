package com.kumpello.whereiseveryone.feature.main.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import com.kumpello.whereiseveryone.feature.main.R

private val avatarCache = LruCache<String, Bitmap>(50)

fun createAvatarBitmap(
    name: String?,
    backgroundColor: Int,
    sizePx: Int = 130
): Bitmap {
    val key = "avatar_${name.orEmpty()}_${backgroundColor}_$sizePx"
    return createCachedMarkerBitmap(key, backgroundColor, sizePx) { canvas ->
        if (!name.isNullOrBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = markerForegroundColor(backgroundColor)
                textAlign = Paint.Align.CENTER
                textSize = sizePx * 0.42f
                val textWidth = measureText(name.uppercase())
                val maxWidth = sizePx * 0.8f
                if (textWidth > maxWidth) {
                    textSize *= maxWidth / textWidth
                }
            }
            val textY = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(name.uppercase(), sizePx / 2f, textY, textPaint)
        }
    }
}

internal fun createCarAvatarBitmap(
    context: android.content.Context,
    backgroundColor: Int,
    sizePx: Int = 130,
): Bitmap = createCachedMarkerBitmap("car_${backgroundColor}_$sizePx", backgroundColor, sizePx) { canvas ->
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_car)!!.mutate()
    val inset = (sizePx * 0.15f).toInt()
    drawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)
    drawable.setTint(markerForegroundColor(backgroundColor))
    drawable.draw(canvas)
}

fun createTintedBitmap(
    context: android.content.Context,
    @androidx.annotation.DrawableRes resourceId: Int,
    tintColor: Int,
    sizePx: Int = 130
): Bitmap {
    val drawable = ContextCompat.getDrawable(context, resourceId)!!.mutate()
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, sizePx, sizePx)
    drawable.setTint(tintColor)
    drawable.draw(canvas)
    return bitmap
}

fun colorForUsername(username: String): Int {
    val hue = (username.hashCode() and 0x7FFFFFFF % 360).toFloat()
    return android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.5f, 0.7f))
}

private fun markerForegroundColor(backgroundColor: Int): Int =
    if (ColorUtils.calculateLuminance(backgroundColor) > 0.179) {
        android.graphics.Color.BLACK
    } else {
        android.graphics.Color.WHITE
    }

private fun createCachedMarkerBitmap(
    key: String,
    backgroundColor: Int,
    sizePx: Int,
    drawCenter: (Canvas) -> Unit,
): Bitmap {
    avatarCache.get(key)?.let { return it }
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint)
    drawCenter(canvas)
    avatarCache.put(key, bitmap)
    return bitmap
}
