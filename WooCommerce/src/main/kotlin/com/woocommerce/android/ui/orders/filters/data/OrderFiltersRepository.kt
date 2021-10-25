package com.woocommerce.android.ui.orders.filters.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderFiltersRepository @Inject constructor() {
    private var selectedOrderFilters: MutableMap<OrderFilterCategory, List<String>> = mutableMapOf()

    fun getCachedFiltersSelection() = selectedOrderFilters

    fun updateSelectedFilters(updatedFilters: MutableMap<OrderFilterCategory, List<String>>) {
        selectedOrderFilters.clear()
        selectedOrderFilters = updatedFilters
    }

    enum class OrderFilterCategory {
        ORDER_STATUS,
        DATE_RANGE
    }
}
