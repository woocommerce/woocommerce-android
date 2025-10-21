package com.woocommerce.android.util

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import java.time.Duration

/**
 * Normalize duration by adjusting for precision issues.
 *
 * This function handles cases where a booking duration is very close to
 * common time boundaries (days/hours) but falls short due to precision issues.
 * It rounds up durations that are within one minute of these boundaries.
 */
fun Duration.normalizeDuration(): Duration {
    val dayInSeconds = Duration.ofDays(1).seconds
    val hourInSeconds = Duration.ofHours(1).seconds
    val minuteInSeconds = Duration.ofMinutes(1).seconds

    var durationInSeconds = this.seconds
    val boundaries = listOf(dayInSeconds, hourInSeconds)
    for (boundary in boundaries) {
        val remainder = durationInSeconds % boundary
        val difference = if (remainder == 0L) 0L else boundary - remainder
        if (difference > 0 && difference <= minuteInSeconds) {
            durationInSeconds += difference
        }
    }
    return Duration.ofSeconds(durationInSeconds)
}

@Suppress("LongMethod")
fun Duration.toHumanReadableFormat(resourceProvider: ResourceProvider): String {
    if (this < Duration.ofMinutes(1)) {
        return resourceProvider.getQuantityString(
            quantity = seconds.toInt(),
            default = R.string.booking_duration_seconds,
            one = R.string.booking_duration_second
        )
    }

    val days = toDays()
    val hours = minusDays(days).toHours()
    val minutes = minusDays(days).minusHours(hours).toMinutes()

    return buildString {
        if (days > 0) {
            append(
                resourceProvider.getQuantityString(
                    quantity = days.toInt(),
                    default = R.string.booking_duration_days,
                    one = R.string.booking_duration_day
                )
            )
        }
        if (hours > 0) {
            append(" ")
            append(
                resourceProvider.getQuantityString(
                    quantity = hours.toInt(),
                    default = R.string.booking_duration_hours,
                    one = R.string.booking_duration_hour
                )
            )
        }
        if (minutes > 0) {
            append(" ")
            append(
                resourceProvider.getQuantityString(
                    quantity = minutes.toInt(),
                    default = R.string.booking_duration_minutes,
                    one = R.string.booking_duration_minute
                )
            )
        }
    }.trim()
}
