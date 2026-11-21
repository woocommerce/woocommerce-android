package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.data.CustomDateRangeDataStore
import com.woocommerce.android.util.CalendarHelper
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.Date
import java.util.Locale

abstract class GetSelectedDateRange(
    private val appPrefs: AppPrefsWrapper,
    private val customDateRangeDataStore: CustomDateRangeDataStore,
    private val dateUtils: DateUtils,
    private val calendarHelper: CalendarHelper
) {
    operator fun invoke(): Flow<StatsTimeRangeSelection> {
        val selectedRangeTypeFlow = appPrefs.observePrefs()
            .map { getSelectedRange() }
            .onStart { emit(getSelectedRange()) }
            .distinctUntilChanged()

        val customRangeFlow = customDateRangeDataStore.dateRange

        val siteDayChanges = flow {
            while (true) {
                val untilMidnight = dateUtils.millisUntilNextSiteDay()
                emit(Unit)
                delay(untilMidnight)
            }
        }
        return combine(selectedRangeTypeFlow, customRangeFlow, siteDayChanges) { selectionType, customRange, _ ->
            val calendar = calendarHelper.getCalendarForSelectedSite()
            when (selectionType) {
                SelectionType.CUSTOM -> {
                    selectionType.generateSelectionData(
                        calendar = calendar,
                        locale = Locale.getDefault(),
                        referenceStartDate = customRange?.start ?: Date(),
                        referenceEndDate = customRange?.end ?: Date()
                    )
                }

                else -> {
                    val today = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
                    selectionType.generateSelectionData(
                        calendar = calendar,
                        locale = Locale.getDefault(),
                        referenceStartDate = today,
                        referenceEndDate = today
                    )
                }
            }
        }
    }

    abstract fun getSelectedRange(): SelectionType
}
