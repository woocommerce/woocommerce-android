package com.woocommerce.android.util

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import java.time.Duration

class DurationExtTest {

    private lateinit var resourceProvider: ResourceProvider

    @Before
    fun setUp() {
        resourceProvider = mock {
            on { getQuantityString(any(), any(), anyOrNull(), anyOrNull()) } doAnswer { invocation ->
                val quantity = invocation.arguments[0] as Int
                val default = invocation.arguments[1] as Int
                val one = invocation.arguments[3] as Int?
                when (quantity) {
                    1 -> when (one) {
                        R.string.booking_duration_second -> "1 second"
                        R.string.booking_duration_minute -> "1 minute"
                        R.string.booking_duration_hour -> "1 hour"
                        R.string.booking_duration_day -> "1 day"
                        else -> ""
                    }
                    else -> when (default) {
                        R.string.booking_duration_seconds -> "$quantity seconds"
                        R.string.booking_duration_minutes -> "$quantity minutes"
                        R.string.booking_duration_hours -> "$quantity hours"
                        R.string.booking_duration_days -> "$quantity days"
                        else -> ""
                    }
                }
            }
        }
    }

    @Test
    fun `given day without less than a minute, when normalizeDuration, then rounds up to nearest day`() {
        val duration = Duration.ofDays(1).minusSeconds(30)
        val expected = Duration.ofDays(1)
        assertEquals(expected, duration.normalizeDuration())
    }

    @Test
    fun `given almost an hour without less than a minute, when normalizeDuration, then rounds up to nearest hour`() {
        val duration = Duration.ofHours(1).minusSeconds(30)
        val expected = Duration.ofHours(1)
        assertEquals(expected, duration.normalizeDuration())
    }

    @Test
    fun `given an hour without more than a minute, when normalizeDuration, then does not change duration`() {
        val duration = Duration.ofHours(1).minusMinutes(2)
        assertEquals(duration, duration.normalizeDuration())
    }

    @Test
    fun `given duration of 45 seconds, when toHumanReadableFormat, then formatted correctly`() {
        val duration = Duration.ofSeconds(45)
        val expected = "45 seconds"
        assertEquals(expected, duration.toHumanReadableFormat(resourceProvider))
    }

    @Test
    fun `given duration of 1 minute, when toHumanReadableFormat, then formatted correctly`() {
        val duration = Duration.ofMinutes(1)
        val expected = "1 minute"
        assertEquals(expected, duration.toHumanReadableFormat(resourceProvider))
    }

    @Test
    fun `given duration of 30 minutes, when toHumanReadableFormat, then formatted correctly`() {
        val duration = Duration.ofMinutes(30)
        val expected = "30 minutes"
        assertEquals(expected, duration.toHumanReadableFormat(resourceProvider))
    }

    @Test
    fun `given duration of 1 hour, when toHumanReadableFormat, then formatted correctly`() {
        val duration = Duration.ofHours(1)
        val expected = "1 hour"
        assertEquals(expected, duration.toHumanReadableFormat(resourceProvider))
    }

    @Test
    fun `given duration of 2 hours, when toHumanReadableFormat, then formatted correctly`() {
        val duration = Duration.ofHours(2)
        val expected = "2 hours"
        assertEquals(expected, duration.toHumanReadableFormat(resourceProvider))
    }

    @Test
    fun `given duration of 1 day, when toHumanReadableFormat, then formatted correctly`() {
        val duration = Duration.ofDays(1)
        val expected = "1 day"
        assertEquals(expected, duration.toHumanReadableFormat(resourceProvider))
    }

    @Test
    fun `given duration of 3 days, when toHumanReadableFormat, then formatted correctly`() {
        val duration = Duration.ofDays(3)
        val expected = "3 days"
        assertEquals(expected, duration.toHumanReadableFormat(resourceProvider))
    }

    @Test
    fun `given duration of 1 day and 2 hours, when toHumanReadableFormat, then formatted correctly`() {
        val duration = Duration.ofDays(1).plusHours(2)
        val expected = "1 day 2 hours"
        assertEquals(expected, duration.toHumanReadableFormat(resourceProvider))
    }

    @Test
    fun `given duration of 1 day, 2 hours and 30 minutes, when toHumanReadableFormat, then formatted correctly`() {
        val duration = Duration.ofDays(1).plusHours(2).plusMinutes(30)
        val expected = "1 day 2 hours 30 minutes"
        assertEquals(expected, duration.toHumanReadableFormat(resourceProvider))
    }
}
