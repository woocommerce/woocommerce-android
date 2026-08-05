package com.woocommerce.android.ui.compose.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class DatePickerDialogTest {
    @Test
    fun `given positive or negative device timezone, when picker date is converted, then calendar day is unchanged`() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            listOf("Pacific/Kiritimati", "America/Adak").forEach { timeZoneId ->
                TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId))
                val date = LocalDate.of(2025, 11, 3)

                val result = date.toUtcDatePickerCalendar().timeInMillis.toUtcLocalDate()

                assertThat(result).isEqualTo(date)
            }
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
