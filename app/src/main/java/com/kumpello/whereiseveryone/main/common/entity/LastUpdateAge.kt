package com.kumpello.whereiseveryone.main.common.entity

enum class LastUpdateAge(val opacity: Double, val displayName: String) {
    OLD_AS_FUCK(0.1, "Old as fuck"),
    OLD(0.3, "Old"),
    SOMEWHAT_OLD(0.5, "Somewhat old"),
    SOMEWHAT_NEW(0.7, "Somewhat new"),
    NEW(0.9, "New"),
    FRESH(1.0, "Fresh")
}