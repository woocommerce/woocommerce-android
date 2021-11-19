package com.woocommerce.android.ui.orders.filters.data

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderFiltersRepository @Inject constructor(
    private val appSharedPrefs: AppPrefsWrapper,
    private val selectedSite: SelectedSite
) {
    fun setSelectedFilters(
        filterCategory: OrderListFilterCategory,
        selectedFilters: List<String>
    ) {
        selectedSite.getIfExists()?.let {
            appSharedPrefs.setOrderFilters(
                it.id,
                filterCategory.name,
                selectedFilters.joinToString(separator = ",")
            )
        }
    }

    fun getCurrentFilterSelection(filterCategory: OrderListFilterCategory): List<String> =
        selectedSite.getIfExists()?.let {
            appSharedPrefs.getOrderFilters(it.id, filterCategory.name)
                ?.split(",")
        } ?: emptyList()
}
