package com.kumpello.whereiseveryone.main.common.domain.usecase

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FormatDateUseCase {
    fun execute(timestamp: Long): String {
        val javaInstant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault())
        return formatter.format(javaInstant)
    }
}
