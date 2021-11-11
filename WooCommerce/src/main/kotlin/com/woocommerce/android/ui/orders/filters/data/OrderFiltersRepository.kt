package com.woocommerce.android.ui.orders.filters.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderFiltersRepository @Inject constructor() {
    private var selectedOrderListFilters: MutableMap<OrderListFilterCategory, List<String>> = mutableMapOf()

    fun getCachedFiltersSelection(): Map<OrderListFilterCategory, List<String>> = selectedOrderListFilters

    fun updateSelectedFilters(filterCategory: OrderListFilterCategory, selectedFilters: List<String>) {
        if (selectedFilters.isEmpty()) {
            selectedOrderListFilters.remove(filterCategory)
        } else {
            selectedOrderListFilters[filterCategory] = selectedFilters
        }
    }
}
