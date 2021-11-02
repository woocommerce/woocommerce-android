package com.woocommerce.android.ui.orders.filters.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderFiltersRepository @Inject constructor() {
    private var selectedOrderListFilters: MutableMap<OrderListFilterCategory, List<String>> = mutableMapOf()

    fun getCachedFiltersSelection(): Map<OrderListFilterCategory, List<String>> = selectedOrderListFilters

    fun updateSelectedFilters(updatedFilters: Map<OrderListFilterCategory, List<String>>) {
        selectedOrderListFilters.clear()
        selectedOrderListFilters = updatedFilters.toMutableMap()
    }

    enum class OrderListFilterCategory {
        ORDER_STATUS,
        DATE_RANGE
    }
}
