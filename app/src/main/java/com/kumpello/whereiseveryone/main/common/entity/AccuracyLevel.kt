package com.kumpello.whereiseveryone.main.common.entity

enum class AccuracyLevel(val haloSize: Double) {
    TRAGIC(50.toDouble()),
    LOW(30.toDouble()),
    MEDIUM(15.toDouble()),
    HIGH(5.toDouble()),
    PERFECT(0.toDouble())
}