package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.ui.orders.filters.data.DateRange
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.domain.getBeforeAndAfterFrom
import com.woocommerce.android.util.DateUtils
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class GetWCOrderListDescriptorWithFiltersBySiteId @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository,
    private val dateUtils: DateUtils,
    private val siteStore: SiteStore
) {
    operator fun invoke(siteId: Long): WCOrderListDescriptor? {
        val selectedDateRange = orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.DATE_RANGE)
            .map { DateRange.fromValue(it) }
            .firstOrNull()
        val rangeStartAndEnd = selectedDateRange.getBeforeAndAfterFrom(orderFiltersRepository, dateUtils)

        val orderStatusFilters = orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.ORDER_STATUS)
            .joinToString(separator = ",")

        val site = siteStore.getSiteBySiteId(siteId) ?: return null

        return WCOrderListDescriptor(
            site = site,
            statusFilter = orderStatusFilters,
            beforeFilter = rangeStartAndEnd.second,
            afterFilter = rangeStartAndEnd.first,
            productId = orderFiltersRepository.productFilter,
            customerId = orderFiltersRepository.customerFilter
        )
    }
}
