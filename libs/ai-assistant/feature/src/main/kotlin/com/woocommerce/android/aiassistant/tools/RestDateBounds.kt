package com.woocommerce.android.aiassistant.tools

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal object RestDateBounds {
    fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }

    fun lowerBound(value: String): String? =
        value.takeIf { parseDate(it) != null }?.let { "${it}T00:00:00" }

    fun upperBound(value: String): String? =
        value.takeIf { parseDate(it) != null }?.let { "${it}T23:59:59" }
}
