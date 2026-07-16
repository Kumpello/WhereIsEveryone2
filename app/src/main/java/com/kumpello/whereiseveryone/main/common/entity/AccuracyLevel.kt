package com.kumpello.whereiseveryone.main.common.entity

enum class AccuracyLevel(val haloSize: Double, val displayName: String) {
    TRAGIC(12.0, "Tragic"),
    LOW(7.5, "Low"),
    MEDIUM(3.7, "Medium"),
    HIGH(1.7, "High"),
    PERFECT(0.0, "Perfect"),

    UNKNOWN((-1.0), "Unknown")
}