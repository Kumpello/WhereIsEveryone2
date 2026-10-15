package com.kumpello.whereiseveryone.feature.main.ui.model

enum class MapFpsLimit(val fps: Int) {
    FPS_30(30),
    FPS_60(60),
    FPS_90(90),
    FPS_120(120);

    fun isSupported(maxRefreshRate: Float): Boolean {
        // Displays can report fractional rates such as 59.94 or 119.88 Hz.
        return fps <= maxRefreshRate + 0.5f
    }

    fun forDisplay(maxRefreshRate: Float): MapFpsLimit {
        return entries.lastOrNull { it.fps <= fps && it.isSupported(maxRefreshRate) } ?: FPS_30
    }

    companion object {
        fun fromPreference(fps: Int?): MapFpsLimit {
            return entries.firstOrNull { it.fps == fps } ?: FPS_120
        }
    }
}
