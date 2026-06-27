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
            0 -> Calendar.SUNDAY
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            else -> null
        }
    }
}
