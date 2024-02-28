package com.woocommerce.android.ui.orders.filters.data

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.CUSTOMER
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.PRODUCT
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderFiltersRepository @Inject constructor(
    private val appSharedPrefs: AppPrefsWrapper,
    private val selectedSite: SelectedSite
) {
    var productFilter: Long? = null

    var customerFilter: Long? = null

    fun setSelectedFilters(
        filterCategory: OrderListFilterCategory,
        selectedFilters: List<String>
    ) {
        when (filterCategory) {
            PRODUCT -> {
                productFilter = selectedFilters.firstOrNull()?.toLongOrNull()
            }
            CUSTOMER -> {
                customerFilter = selectedFilters.firstOrNull()?.toLongOrNull()
            }
            else -> {
                selectedSite.getIfExists()?.let {
                    appSharedPrefs.setOrderFilters(
                        it.id,
                        filterCategory.name,
                        selectedFilters.joinToString(separator = ",")
                    )
                }
            }
        }
    }

    fun getCurrentFilterSelection(filterCategory: OrderListFilterCategory): List<String> {
        val preferenceFilters = selectedSite.getIfExists()?.let { site ->
            appSharedPrefs.getOrderFilters(site.id, filterCategory.name)
                .split(",")
                .filter { it.isNotBlank() }
        } ?: emptyList()
        return preferenceFilters + getProductFilter(filterCategory) + getCustomerFilter(filterCategory)
    }

    private fun getProductFilter(filterCategory: OrderListFilterCategory) =
        if (filterCategory == PRODUCT) listOfNotNull(productFilter?.toString()) else emptyList()

    private fun getCustomerFilter(filterCategory: OrderListFilterCategory) =
        if (filterCategory == CUSTOMER) listOfNotNull(customerFilter?.toString()) else emptyList()

    fun getCustomDateRangeFilter(): Pair<Long, Long> =
        selectedSite.getIfExists()?.let {
            appSharedPrefs.getOrderFilterCustomDateRange(it.id)
        } ?: Pair(0, 0)

    fun setCustomDateRange(startDateMillis: Long, endDateMillis: Long) {
        selectedSite.getIfExists()?.let {
            appSharedPrefs.setOrderFilterCustomDateRange(
                it.id,
                startDateMillis,
                endDateMillis
            )
        }
    }
}
