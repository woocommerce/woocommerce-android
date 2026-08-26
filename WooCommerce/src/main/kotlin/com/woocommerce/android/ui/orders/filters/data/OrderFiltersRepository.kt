package com.woocommerce.android.ui.orders.filters.data

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.extensions.toDateAtStartOfDay
import com.woocommerce.android.extensions.toEpochDay
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.CUSTOMER
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.PRODUCT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCCustomerStore
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderFiltersRepository @Inject constructor(
    private val appSharedPrefs: AppPrefsWrapper,
    private val customerStore: WCCustomerStore,
    private val selectedSite: SelectedSite,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    var productFilter: Long? = null

    var customerFilter: Long? = null

    init {
        selectedSite.observe()
            .distinctUntilChanged { old, new -> old?.id == new?.id }
            .onEach {
                productFilter = null
                customerFilter = null
            }.launchIn(appCoroutineScope)
    }

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

    fun loadCustomerInfoIfNeeded(customerId: Long) {
        appCoroutineScope.launch {
            if (customerStore.getCustomerByRemoteId(selectedSite.get(), customerId) == null) {
                customerStore.fetchSingleCustomer(selectedSite.get(), customerId)
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

    fun getCustomDateRangeDays(): Pair<Long, Long> =
        selectedSite.getIfExists()?.let { site ->
            val (startDay, endDay) = appSharedPrefs.getOrderFilterCustomDateRangeDays(site.id)
            if (startDay != 0L || endDay != 0L) {
                // Drops the legacy millis prefs if an interrupted migration left them behind. Delete in 25.9,
                // see WOOMOB-3841.
                appSharedPrefs.removeOrderFilterCustomDateRange(site.id)
                Pair(startDay, endDay)
            } else {
                migrateLegacyCustomDateRange(site.id)
            }
        } ?: Pair(0, 0)

    fun getCustomDateRangeFilter(): Pair<Long, Long> {
        val (startDay, endDay) = getCustomDateRangeDays()
        return if (startDay != 0L || endDay != 0L) {
            Pair(startDay.toDateAtStartOfDay().time, endDay.toDateAtStartOfDay().time)
        } else {
            Pair(0, 0)
        }
    }

    fun setCustomDateRange(startDay: Long, endDay: Long) {
        selectedSite.getIfExists()?.let { site ->
            appSharedPrefs.setOrderFilterCustomDateRangeDays(site.id, startDay, endDay)
        }
    }

    // Migrates ranges saved before 25.5 from the millis prefs. Delete in 25.9, see WOOMOB-3841.
    private fun migrateLegacyCustomDateRange(siteId: Int): Pair<Long, Long> {
        val (startMillis, endMillis) = appSharedPrefs.getOrderFilterCustomDateRange(siteId)
        if (startMillis == 0L && endMillis == 0L) return Pair(0, 0)
        // The legacy millis are instants, so we read the days in the current device timezone
        val startDay = Date(startMillis).toEpochDay()
        val endDay = Date(endMillis).toEpochDay()
        appSharedPrefs.setOrderFilterCustomDateRangeDays(siteId, startDay, endDay)
        appSharedPrefs.removeOrderFilterCustomDateRange(siteId)
        return Pair(startDay, endDay)
    }
}
