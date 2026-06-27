package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WPSettingsStore
import java.util.Calendar
import javax.inject.Inject

class SiteWeekStartCalendarProvider @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wpSettingsStore: WPSettingsStore
) {
    fun getCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        val site = selectedSite.getOrNull() ?: return calendar
        val firstDayOfWeek = wpSettingsStore.getStartOfWeek(site)?.toCalendarFirstDayOfWeek() ?: return calendar
        return calendar.apply { this.firstDayOfWeek = firstDayOfWeek }
    }

    private fun Int.toCalendarFirstDayOfWeek(): Int? {
        return when (this) {
            WP_WEEK_START_SUNDAY -> Calendar.SUNDAY
            WP_WEEK_START_MONDAY -> Calendar.MONDAY
            WP_WEEK_START_TUESDAY -> Calendar.TUESDAY
            WP_WEEK_START_WEDNESDAY -> Calendar.WEDNESDAY
            WP_WEEK_START_THURSDAY -> Calendar.THURSDAY
            WP_WEEK_START_FRIDAY -> Calendar.FRIDAY
            WP_WEEK_START_SATURDAY -> Calendar.SATURDAY
            else -> null
        }
    }

    private companion object {
        const val WP_WEEK_START_SUNDAY = 0
        const val WP_WEEK_START_MONDAY = 1
        const val WP_WEEK_START_TUESDAY = 2
        const val WP_WEEK_START_WEDNESDAY = 3
        const val WP_WEEK_START_THURSDAY = 4
        const val WP_WEEK_START_FRIDAY = 5
        const val WP_WEEK_START_SATURDAY = 6
    }
}
