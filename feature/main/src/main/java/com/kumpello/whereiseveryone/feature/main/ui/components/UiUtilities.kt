package com.kumpello.whereiseveryone.feature.main.ui.components

import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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

@Composable
fun rememberMaxDisplayRefreshRate(): Float {
    val context = LocalContext.current
    val view = LocalView.current
    var maxRefreshRate by remember(view) { mutableFloatStateOf(view.maxDisplayRefreshRate()) }

    DisposableEffect(context, view) {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                maxRefreshRate = view.maxDisplayRefreshRate()
            }

            override fun onDisplayRemoved(displayId: Int) {
                maxRefreshRate = view.maxDisplayRefreshRate()
            }

            override fun onDisplayChanged(displayId: Int) {
                maxRefreshRate = view.maxDisplayRefreshRate()
            }
        }
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                maxRefreshRate = view.maxDisplayRefreshRate()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        }
        view.addOnAttachStateChangeListener(attachListener)
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        maxRefreshRate = view.maxDisplayRefreshRate()
        onDispose {
            view.removeOnAttachStateChangeListener(attachListener)
            displayManager.unregisterDisplayListener(listener)
        }
    }

    return maxRefreshRate
}

private fun View.maxDisplayRefreshRate(): Float {
    return display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: 60f
}
