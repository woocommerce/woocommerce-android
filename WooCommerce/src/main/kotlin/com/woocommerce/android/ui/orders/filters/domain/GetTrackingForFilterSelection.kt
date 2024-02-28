package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.CUSTOMER
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.PRODUCT
import javax.inject.Inject

class GetTrackingForFilterSelection @Inject constructor(
    private val orderFiltersRepository: OrderFiltersRepository
) {
    operator fun invoke(): Map<String, String> {
        val orderStatusOptions = orderFiltersRepository
            .getCurrentFilterSelection(ORDER_STATUS)
        val dateRangeOptions = orderFiltersRepository.getCurrentFilterSelection(DATE_RANGE)
        val productFilterOptions = orderFiltersRepository.getCurrentFilterSelection(PRODUCT)
        val customerFilterOptions = orderFiltersRepository.getCurrentFilterSelection(CUSTOMER)

        val trackingData = mutableMapOf<String, String>()
        if (orderStatusOptions.isNotEmpty()) {
            trackingData[AnalyticsTracker.KEY_STATUS] = orderStatusOptions.joinToString(separator = ",")
        }
        if (dateRangeOptions.isNotEmpty()) {
            trackingData[AnalyticsTracker.KEY_DATE_RANGE] = dateRangeOptions.first()
        }
        if (productFilterOptions.isNotEmpty()) {
            trackingData[AnalyticsTracker.KEY_PRODUCT] = productFilterOptions.first()
        }
        if (customerFilterOptions.isNotEmpty()) {
            trackingData[AnalyticsTracker.KEY_CUSTOMER] = customerFilterOptions.first()
        }
        return trackingData
    }
}
