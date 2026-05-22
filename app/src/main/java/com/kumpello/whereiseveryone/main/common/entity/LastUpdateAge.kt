package com.kumpello.whereiseveryone.main.common.entity

enum class LastUpdateAge(val opacity: Double) {
    OLD_AS_FUCK(0.1),
    OLD(0.3),
    SOMEWHAT_OLD(0.5),
    SOMEWHAT_NEW(0.7),
    NEW(0.9),
    FRESH(1.toDouble())
}