package com.woocommerce.android.ui.orders.filters.data

import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_2_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.THIS_MONTH
import com.woocommerce.android.ui.orders.filters.data.DateRange.THIS_WEEK
import com.woocommerce.android.ui.orders.filters.data.DateRange.TODAY
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.list.OrderListRepository
import javax.inject.Inject

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
        orderFiltersRepository.getCachedFiltersSelection()[ORDER_STATUS]
            ?.contains(filterKey) ?: false
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
        orderFiltersRepository.getCachedFiltersSelection()[DATE_RANGE]
            ?.contains(filterKey) ?: false
}
