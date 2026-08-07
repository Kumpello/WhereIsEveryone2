package com.kumpello.whereiseveryone.main.map.extension

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap

private val avatarCache = LruCache<String, Bitmap>(50)

fun createAvatarBitmap(
    name: String?,
    backgroundColor: Int,
    sizePx: Int = 130
): Bitmap {
    val key = "${name.orEmpty()}_${backgroundColor}_$sizePx"
    avatarCache.get(key)?.let { return it }

    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, circlePaint)
    if (!name.isNullOrBlank()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
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
    avatarCache.put(key, bitmap)
    return bitmap
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
