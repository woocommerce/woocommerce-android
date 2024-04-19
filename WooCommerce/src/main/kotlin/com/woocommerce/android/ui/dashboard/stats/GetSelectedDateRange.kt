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
    operator fun invoke(statsViewType: StatsViewType): Flow<StatsTimeRangeSelection> {
        val selectedRangeTypeFlow = appPrefs.observePrefs()
            .map { getSelectedRangeFor(statsViewType) }
            .onStart { emit(getSelectedRangeFor(statsViewType)) }
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

    private fun getSelectedRangeFor(statsViewType: StatsViewType): SelectionType {
        val previouslySelectedTab = when (statsViewType) {
            StatsViewType.STORE_STATS -> appPrefs.getActiveStoreStatsTab()
            StatsViewType.TOP_PERFORMERS -> appPrefs.getActiveTopPerformersGranularity()
        }
        return runCatching {
            SelectionType.valueOf(previouslySelectedTab)
        }.getOrDefault(SelectionType.TODAY)
    }

    enum class StatsViewType {
        STORE_STATS,
        TOP_PERFORMERS,
    }
}
