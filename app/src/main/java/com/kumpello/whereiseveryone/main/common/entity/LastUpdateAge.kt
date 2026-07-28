package com.kumpello.whereiseveryone.main.common.entity

enum class LastUpdateAge(val opacity: Double, val displayName: String) {
    OLD_AS_FUCK(0.3, "Old as fuck"),
    OLD(0.45, "Old"),
    SOMEWHAT_OLD(0.6, "Somewhat old"),
    SOMEWHAT_NEW(0.75, "Somewhat new"),
    NEW(0.9, "New"),
    FRESH(1.0, "Fresh")
}