package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.ui.orders.filters.data.DateRange
import com.woocommerce.android.ui.orders.filters.data.DateRange.CUSTOM_RANGE
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_2_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_30_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_7_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.TODAY
import com.woocommerce.android.ui.orders.filters.data.DateRangeFilterOption
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import javax.inject.Inject

class GetDateRangeFilterOptions @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository
) {
    operator fun invoke(): List<DateRangeFilterOption> =
        listOf(TODAY, LAST_2_DAYS, LAST_7_DAYS, LAST_30_DAYS, CUSTOM_RANGE)
            .map {
                when (it) {
                    TODAY,
                    LAST_2_DAYS,
                    LAST_7_DAYS,
                    LAST_30_DAYS -> it.toDateRangeFilterOption()
                    CUSTOM_RANGE -> it.toCustomDateRangeFilterOption()
                }
            }

    private fun checkIfSelected(filterKey: String): Boolean =
        orderFiltersRepository
            .getCurrentFilterSelection(OrderListFilterCategory.DATE_RANGE)
            .contains(filterKey)

    private fun DateRange.toDateRangeFilterOption() =
        DateRangeFilterOption(
            dateRange = this,
            isSelected = checkIfSelected(filterKey),
            startDate = 0,
            endDate = 0
        )

    private fun DateRange.toCustomDateRangeFilterOption(): DateRangeFilterOption {
        val selectedDateRange = orderFiltersRepository.getCustomDateRangeFilter()
        return DateRangeFilterOption(
            dateRange = this,
            isSelected = checkIfSelected(filterKey),
            startDate = selectedDateRange.first,
            endDate = selectedDateRange.second
        )
    }
}
