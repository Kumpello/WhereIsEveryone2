package com.kumpello.whereiseveryone.main.common.entity

enum class AccuracyLevel(val haloSize: Double, val displayName: String) {
    TRAGIC(6.0, "Tragic"),
    LOW(4.0, "Low"),
    MEDIUM(2.5, "Medium"),
    HIGH(1.0, "High"),
    PERFECT(0.0, "Perfect"),

    UNKNOWN((-1.0), "Unknown")
}