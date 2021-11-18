package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.filters.data.DateRange
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.util.DateUtils
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import javax.inject.Inject

class GetWCOrderListDescriptorWithFilters @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository,
    private val selectedSite: SelectedSite,
    private val dateUtils: DateUtils
) {
    operator fun invoke(): WCOrderListDescriptor {
        val dateRangeAfterFilter = orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.DATE_RANGE)
            .map { DateRange.fromValue(it) }
            .firstOrNull()
            ?.toAfterIso8061DateString(dateUtils)

        val orderStatusFilters =
            orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.ORDER_STATUS)
                .joinToString(separator = ",")

        return WCOrderListDescriptor(
            site = selectedSite.get(),
            statusFilter = orderStatusFilters,
            afterFilter = dateRangeAfterFilter
        )
    }

    private fun DateRange.toAfterIso8061DateString(dateUtils: DateUtils): String? {
        val afterDate = when (this) {
            DateRange.TODAY -> dateUtils.getDateForTodayAtTheStartOfTheDay()
            DateRange.LAST_2_DAYS -> dateUtils.getCurrentDateTimeMinusDays(days = 2)
            DateRange.LAST_7_DAYS -> dateUtils.getCurrentDateTimeMinusDays(days = 7)
            DateRange.LAST_30_DAYS -> dateUtils.getCurrentDateTimeMinusDays(days = 30)
        }
        return dateUtils.toIso8601Format(afterDate)
    }
}
