package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WPSettingsStore
import java.time.DayOfWeek
import java.util.Calendar
import javax.inject.Inject

class CalendarHelper @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wpSettingsStore: WPSettingsStore
) {
    suspend fun getCalendarForSelectedSite(): Calendar {
        val calendar = Calendar.getInstance()
        val site = selectedSite.getOrNull() ?: return calendar
        val firstDayOfWeek = wpSettingsStore.getStartOfWeek(site)?.toCalendarFirstDayOfWeek() ?: return calendar
        return calendar.apply { this.firstDayOfWeek = firstDayOfWeek }
    }

    private fun DayOfWeek.toCalendarFirstDayOfWeek(): Int {
        return when (this) {
            DayOfWeek.SUNDAY -> Calendar.SUNDAY
            DayOfWeek.MONDAY -> Calendar.MONDAY
            DayOfWeek.TUESDAY -> Calendar.TUESDAY
            DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
            DayOfWeek.THURSDAY -> Calendar.THURSDAY
            DayOfWeek.FRIDAY -> Calendar.FRIDAY
            DayOfWeek.SATURDAY -> Calendar.SATURDAY
        }
    }
}
