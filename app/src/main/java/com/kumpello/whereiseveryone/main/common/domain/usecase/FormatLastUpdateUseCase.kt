package com.kumpello.whereiseveryone.main.common.domain.usecase

import kotlin.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant as JavaInstant

class FormatLastUpdateUseCase {
    fun execute(instant: Instant): String {
        val javaInstant = JavaInstant.parse(instant.toString())
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
            .withZone(ZoneId.systemDefault())
        return formatter.format(javaInstant)
    }
}
