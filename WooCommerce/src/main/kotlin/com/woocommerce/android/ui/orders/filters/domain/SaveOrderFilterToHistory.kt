package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.R
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.filters.FilterHistoryRepository
import com.woocommerce.android.ui.filters.FilterHistoryType
import com.woocommerce.android.ui.orders.filters.OrderFilterHistoryMapper
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.CUSTOMER
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.PRODUCT
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.SALES_CHANNEL
import com.woocommerce.android.ui.orders.filters.data.SalesChannel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel.Companion.DEFAULT_ALL_KEY
import com.woocommerce.android.ui.orders.filters.model.toOrderFilterOptionUiModel
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCCustomerStore
import javax.inject.Inject

/**
 * Persists the current order filter selection to the filter history.
 *
 * The current selection lives in [OrderFiltersRepository] (the source of truth once either the
 * categories screen or an options sub-screen has saved it), so this reads from there rather than from
 * a specific ViewModel's in-memory state. That's why it can be invoked from both "Show Orders"
 * entry points and always capture the complete selection.
 */
class SaveOrderFilterToHistory @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val orderFiltersRepository: OrderFiltersRepository,
    private val getOrderStatusFilterOptions: GetOrderStatusFilterOptions,
    private val getDateRangeFilterOptions: GetDateRangeFilterOptions,
    private val dateUtils: DateUtils,
    private val resourceProvider: ResourceProvider,
    private val productListRepository: ProductListRepository,
    private val customerStore: WCCustomerStore,
    private val selectedSite: SelectedSite,
    private val filterHistoryRepository: FilterHistoryRepository,
    private val orderFilterHistoryMapper: OrderFilterHistoryMapper,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    /**
     * Fire-and-forget: the save runs on the application scope so it survives the filter screen being
     * dismissed, and callers can navigate away immediately without awaiting the DB write. Failures are
     * logged rather than propagated — persisting history is best-effort and must never crash the app.
     */
    operator fun invoke() {
        appCoroutineScope.launch {
            runCatching { save() }
                .onFailure { WooLog.e(WooLog.T.ORDERS, "Failed to save order filter to history", it) }
        }
    }

    private suspend fun save() {
        if (!featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)) return
        val selectedCategories = buildSelectedCategories()
        val hasSelection = selectedCategories.any { category ->
            category.orderFilterOptions.any { it.isSelected && it.key != DEFAULT_ALL_KEY }
        }
        if (!hasSelection) return
        // Store the range in epoch days, matching the unit setCustomDateRange expects on restore
        // (getCustomDateRangeFilter returns millis, which would corrupt the days-based pref).
        val customDateRange = orderFiltersRepository.getCustomDateRangeDays()
        filterHistoryRepository.save(
            type = FilterHistoryType.ORDERS,
            payload = orderFilterHistoryMapper.toPayload(
                categories = selectedCategories,
                customDateRangeStart = customDateRange.first,
                customDateRangeEnd = customDateRange.second
            ),
            readableString = orderFilterHistoryMapper.toReadableString(selectedCategories)
        )
    }

    private suspend fun buildSelectedCategories(): List<OrderFilterCategoryUiModel> = listOf(
        selectedCategory(
            ORDER_STATUS,
            // Use the bare status label (not toOrderFilterOptionUiModel, which appends the status count)
            // so the saved history label reads e.g. "Processing" rather than "Processing (12)".
            getOrderStatusFilterOptions().map {
                OrderFilterOptionUiModel(key = it.key, displayName = it.label, isSelected = it.isSelected)
            }
        ),
        selectedCategory(
            DATE_RANGE,
            getDateRangeFilterOptions().map { it.toOrderFilterOptionUiModel(resourceProvider, dateUtils) }
        ),
        selectedCategory(PRODUCT, productFilterOptions()),
        selectedCategory(CUSTOMER, customerFilterOptions()),
        selectedCategory(SALES_CHANNEL, salesChannelFilterOptions())
    )

    private fun selectedCategory(
        categoryKey: OrderListFilterCategory,
        options: List<OrderFilterOptionUiModel>
    ) = OrderFilterCategoryUiModel(
        categoryKey = categoryKey,
        displayName = "",
        displayValue = "",
        orderFilterOptions = options.filter { it.isSelected }
    )

    private fun productFilterOptions(): List<OrderFilterOptionUiModel> = listOfNotNull(
        orderFiltersRepository.productFilter?.let { productId ->
            OrderFilterOptionUiModel(
                key = productId.toString(),
                displayName = productListRepository.getProduct(productId)?.name ?: fallbackDisplayValue(productId),
                isSelected = true
            )
        }
    )

    private suspend fun customerFilterOptions(): List<OrderFilterOptionUiModel> = listOfNotNull(
        orderFiltersRepository.customerFilter?.let { customerId ->
            val name = customerStore.getCustomerByRemoteId(selectedSite.get(), customerId)
                ?.let { customer ->
                    (customer.firstName + " " + customer.lastName)
                        .ifBlank { customer.email }
                        .ifBlank { customer.username }
                } ?: fallbackDisplayValue(customerId)
            OrderFilterOptionUiModel(key = customerId.toString(), displayName = name, isSelected = true)
        }
    )

    private fun salesChannelFilterOptions(): List<OrderFilterOptionUiModel> =
        orderFiltersRepository.getCurrentFilterSelection(SALES_CHANNEL).mapNotNull { key ->
            val displayName = salesChannelDisplayName(key) ?: return@mapNotNull null
            OrderFilterOptionUiModel(key = key, displayName = displayName, isSelected = true)
        }

    private fun salesChannelDisplayName(key: String): String? = when (key) {
        SalesChannel.POS.key -> resourceProvider.getString(R.string.point_of_sale)
        SalesChannel.WEB_CHECKOUT.key ->
            resourceProvider.getString(R.string.orderfilters_sales_channel_filter_web_checkout)
        SalesChannel.WP_ADMIN.key -> resourceProvider.getString(R.string.orderfilters_sales_channel_filter_wp_admin)
        else -> null
    }

    private fun fallbackDisplayValue(id: Long): String =
        resourceProvider.getString(R.string.orderfilters_selected_filter_fallback_display_value, id)
}
