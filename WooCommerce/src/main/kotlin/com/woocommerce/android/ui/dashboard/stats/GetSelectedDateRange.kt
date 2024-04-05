package com.woocommerce.android.ui.dashboard.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.mystore.data.CustomDateRangeDataStore
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetSelectedDateRange @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val customDateRangeDataStore: CustomDateRangeDataStore,
    private val dateUtils: DateUtils
) {
    operator fun invoke(): Flow<StatsTimeRangeSelection> {
        val selectedRangeTypeFlow = appPrefs.observePrefs()
            .map { getSelectedRangeTypeIfAny() }
            .onStart { emit(getSelectedRangeTypeIfAny()) }
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

    private fun getSelectedRangeTypeIfAny(): SelectionType {
        val previouslySelectedTab = appPrefs.getActiveStatsTab()
        return runCatching {
            SelectionType.valueOf(previouslySelectedTab)
        }.getOrDefault(SelectionType.TODAY)
    }
}
