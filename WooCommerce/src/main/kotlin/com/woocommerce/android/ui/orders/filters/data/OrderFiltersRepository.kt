package com.woocommerce.android.ui.orders.filters.data

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.PRODUCT
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderFiltersRepository @Inject constructor(
    private val appSharedPrefs: AppPrefsWrapper,
    private val selectedSite: SelectedSite
) {
    var productFilter: Long? = null
        get() = field
        set(value) {
            field = value
        }

    fun setSelectedFilters(
        filterCategory: OrderListFilterCategory,
        selectedFilters: List<String>
    ) {
        when (filterCategory) {
            PRODUCT -> {
                productFilter = selectedFilters.firstOrNull()?.toLongOrNull()
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

    fun getCurrentFilterSelection(filterCategory: OrderListFilterCategory): List<String> =
        (selectedSite.getIfExists()?.let { site ->
            appSharedPrefs.getOrderFilters(site.id, filterCategory.name)
                .split(",")
                .filter { it.isNotBlank() }
        } ?: emptyList()) + getProductFilter(filterCategory)

    private fun getProductFilter(filterCategory: OrderListFilterCategory) =
        if (filterCategory == PRODUCT) listOfNotNull(productFilter?.toString()) else emptyList()

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
