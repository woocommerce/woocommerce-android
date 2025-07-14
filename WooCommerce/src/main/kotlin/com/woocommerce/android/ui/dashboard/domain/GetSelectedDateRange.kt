package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.dashboard.data.CustomDateRangeDataStore
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.Calendar
import java.util.Date
import java.util.Locale

abstract class GetSelectedDateRange(
    private val appPrefs: AppPrefsWrapper,
    private val customDateRangeDataStore: CustomDateRangeDataStore,
    private val dateUtils: DateUtils
) {
    operator fun invoke(): Flow<StatsTimeRangeSelection> {
        val selectedRangeTypeFlow = appPrefs.observePrefs()
            .map { getSelectedRange() }
            .onStart { emit(getSelectedRange()) }
            .distinctUntilChanged()

        val customRangeFlow = customDateRangeDataStore.dateRange

        return combine(selectedRangeTypeFlow, customRangeFlow) { selectionType, customRange ->
            when (selectionType) {
                CUSTOM -> {
                    selectionType.generateSelectionData(
                        calendar = Calendar.getInstance(),
                        locale = Locale.getDefault(),
                        referenceStartDate = customRange?.start ?: Date(),
                        referenceEndDate = customRange?.end ?: Date()
                    )
                }

                else -> {
                    selectionType.generateSelectionData(
                        calendar = Calendar.getInstance(),
                        locale = Locale.getDefault(),
                        referenceStartDate = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
                        referenceEndDate = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
                    )
                }
            }
        }
    }

    abstract fun getSelectedRange(): SelectionType
}
