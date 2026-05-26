package com.kumpello.whereiseveryone.main.common.ui

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

@Composable
fun rememberBitmapFromDrawable(
    @DrawableRes drawableRes: Int
): Bitmap {
    val context = LocalContext.current

    return remember(drawableRes) {
        val drawable = ContextCompat.getDrawable(context, drawableRes)
            ?: error("Drawable not found")

        drawable.toBitmap()
    }
}