package com.woocommerce.android.ui.orders.filters.data

import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_2_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.THIS_MONTH
import com.woocommerce.android.ui.orders.filters.data.DateRange.THIS_WEEK
import com.woocommerce.android.ui.orders.filters.data.DateRange.TODAY
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.util.DateUtils
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import javax.inject.Inject

class GetWCOrderListDescriptorWithFilters @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository,
    private val selectedSite: SelectedSite,
    private val dateUtils: DateUtils
) {
    operator fun invoke(): WCOrderListDescriptor {
        val dateRangeAfterFilter = orderFiltersRepository.getCurrentFilterSelection(DATE_RANGE)
            .map { DateRange.fromValue(it) }
            .firstOrNull()
            ?.toAfterIso8061DateString(dateUtils)

        val orderStatusFilters =
            orderFiltersRepository.getCurrentFilterSelection(ORDER_STATUS)
                .joinToString(separator = ",")

        return WCOrderListDescriptor(
            site = selectedSite.get(),
            statusFilter = orderStatusFilters,
            afterFilter = dateRangeAfterFilter
        )
    }

    private fun DateRange.toAfterIso8061DateString(dateUtils: DateUtils): String? {
        val afterDate = when (this) {
            TODAY -> dateUtils.getDateForTodayAtTheStartOfTheDay()
            LAST_2_DAYS -> dateUtils.getCurrentDateTimeMinusDays(2)
            THIS_WEEK -> dateUtils.getDateForFirstDayOfCurrentWeek()
            THIS_MONTH -> dateUtils.getDateForFirstDayOfCurrentMonth()
        }
        return dateUtils.toIso8601Format(afterDate)
    }
}

class GetSelectedOrderFiltersCount @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository
) {
    operator fun invoke(): Int =
        (orderFiltersRepository.getCurrentFilterSelection(ORDER_STATUS) +
            orderFiltersRepository.getCurrentFilterSelection(DATE_RANGE)
            ).size
}

class GetOrderStatusFilterOptions @Inject constructor(
    private val orderListRepository: OrderListRepository,
    private val orderFiltersRepository: OrderFiltersRepository
) {
    suspend operator fun invoke(): List<OrderStatusOption> {
        var orderStatus = orderListRepository.getCachedOrderStatusOptions()
        if (orderStatus.isEmpty()) {
            when (orderListRepository.fetchOrderStatusOptionsFromApi()) {
                RequestResult.SUCCESS -> orderStatus = orderListRepository.getCachedOrderStatusOptions()
                else -> { /* do nothing */
                }
            }
        }
        return orderStatus.values
            .toList()
            .map {
                OrderStatusOption(
                    key = it.statusKey,
                    label = it.label,
                    statusCount = it.statusCount,
                    isSelected = checkIfSelected(it.statusKey)
                )
            }
    }

    private fun checkIfSelected(filterKey: String): Boolean =
        orderFiltersRepository
            .getCurrentFilterSelection(ORDER_STATUS)
            .contains(filterKey)
}

class GetDateRangeFilterOptions @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository
) {
    operator fun invoke(): List<DateRangeFilterOption> =
        listOf(TODAY, LAST_2_DAYS, THIS_WEEK, THIS_MONTH)
            .map {
                DateRangeFilterOption(
                    dateRange = it,
                    isSelected = checkIfSelected(it.filterKey)
                )
            }

    private fun checkIfSelected(filterKey: String): Boolean =
        orderFiltersRepository
            .getCurrentFilterSelection(DATE_RANGE)
            .contains(filterKey)
}
