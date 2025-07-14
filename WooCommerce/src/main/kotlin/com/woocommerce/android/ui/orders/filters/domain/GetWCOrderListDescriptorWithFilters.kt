package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.filters.data.DateRange
import com.woocommerce.android.ui.orders.filters.data.DateRange.CUSTOM_RANGE
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_2_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_30_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_7_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.TODAY
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.util.DateUtils
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import javax.inject.Inject

class GetWCOrderListDescriptorWithFilters @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository,
    private val selectedSite: SelectedSite,
    private val dateUtils: DateUtils
) {
    operator fun invoke(): WCOrderListDescriptor {
        val selectedDateRange = orderFiltersRepository.getCurrentFilterSelection(DATE_RANGE)
            .map { DateRange.fromValue(it) }
            .firstOrNull()
        val rangeStartAndEnd = selectedDateRange.getBeforeAndAfterFrom(orderFiltersRepository, dateUtils)

        val orderStatusFilters = orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.ORDER_STATUS)
            .joinToString(separator = ",")

        return WCOrderListDescriptor(
            site = selectedSite.get(),
            statusFilter = orderStatusFilters,
            beforeFilter = rangeStartAndEnd.second,
            afterFilter = rangeStartAndEnd.first,
            productId = orderFiltersRepository.productFilter,
            customerId = orderFiltersRepository.customerFilter
        )
    }
}

fun DateRange?.getBeforeAndAfterFrom(
    orderFiltersRepository: OrderFiltersRepository,
    dateUtils: DateUtils
): Pair<String?, String?> =
    when {
        this == null -> Pair(null, null)
        this != CUSTOM_RANGE -> {
            Pair(dateUtils.toIso8601Format(this.toDateTimeInMillis(dateUtils)), null)
        }

        else -> {
            val customDateRange = orderFiltersRepository.getCustomDateRangeFilter()
            var afterDate: String? = null
            var beforeDate: String? = null
            if (customDateRange.first > 0) {
                afterDate = dateUtils.toIso8601Format(
                    dateUtils.getDateInMillisAtTheStartOfTheDay(customDateRange.first)
                )
            }
            if (customDateRange.second > 0) {
                beforeDate = dateUtils.toIso8601Format(
                    dateUtils.getDateInMillisAtTheEndOfTheDay(customDateRange.second)
                )
            }
            Pair(afterDate, beforeDate)
        }
    }

fun DateRange.toDateTimeInMillis(dateUtils: DateUtils): Long =
    when (this) {
        TODAY -> dateUtils.getDateForTodayAtTheStartOfTheDay()
        LAST_2_DAYS -> dateUtils.getCurrentDateTimeMinusDays(days = 2)
        LAST_7_DAYS -> dateUtils.getCurrentDateTimeMinusDays(days = 7)
        LAST_30_DAYS -> dateUtils.getCurrentDateTimeMinusDays(days = 30)
        CUSTOM_RANGE -> 0
    }
