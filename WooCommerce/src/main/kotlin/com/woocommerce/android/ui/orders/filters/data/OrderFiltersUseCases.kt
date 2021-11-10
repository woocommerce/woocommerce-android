package com.woocommerce.android.ui.orders.filters.data

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.model.OrderFilterDateRangeUiModel
import com.woocommerce.android.ui.orders.filters.model.toAfterIso8061DateString
import com.woocommerce.android.util.DateUtils
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import javax.inject.Inject

class GetWCOrderListDescriptorWithFilters @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository,
    private val selectedSite: SelectedSite,
    private val dateUtils: DateUtils
) {
    operator fun invoke(): WCOrderListDescriptor {
        val selectedFilters = orderFiltersRepository.getCachedFiltersSelection()
        val dateRangeAfterFilter = selectedFilters[DATE_RANGE]
            ?.map { OrderFilterDateRangeUiModel.fromValue(it) }
            ?.first()
            ?.toAfterIso8061DateString(dateUtils)

        return WCOrderListDescriptor(
            site = selectedSite.get(),
            statusFilter = selectedFilters[ORDER_STATUS]?.joinToString(separator = ","),
            afterFilter = dateRangeAfterFilter
        )
    }
}

class GetSelectedOrderFiltersCount @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository
) {
    operator fun invoke(): Int =
        orderFiltersRepository.getCachedFiltersSelection().values
            .map { it.count() }
            .sum()
}
