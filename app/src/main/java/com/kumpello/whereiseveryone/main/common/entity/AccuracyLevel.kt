package com.kumpello.whereiseveryone.main.common.entity

enum class AccuracyLevel(val haloSize: Double, val displayName: String) {
    TRAGIC(1.5, "Tragic"),
    LOW(1.3, "Low"),
    MEDIUM(1.15, "Medium"),
    HIGH(1.0, "High"),
    PERFECT(0.9, "Perfect"),

    UNKNOWN((3.0), "Unknown") //Probably won't occur
}