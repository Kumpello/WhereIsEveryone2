package com.kumpello.whereiseveryone.main.common.domain.usecase

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FormatLastUpdateUseCase {
    fun execute(timestamp: Long): String {
        val javaInstant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
            .withZone(ZoneId.systemDefault())
        return formatter.format(javaInstant)
    }
}
