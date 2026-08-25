package com.woocommerce.android.ui.orders.filters

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.filters.SavedFilter
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.CUSTOMER
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.PRODUCT
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.SALES_CHANNEL
import com.woocommerce.android.ui.orders.filters.data.SalesChannel
import com.woocommerce.android.ui.orders.filters.domain.GetDateRangeFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.domain.IsSalesChannelFilterSupported
import com.woocommerce.android.ui.orders.filters.domain.SaveOrderFilterToHistory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OpenFilterHistory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.ShowFilterOptionsForCategory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.addFilterOptionAll
import com.woocommerce.android.ui.orders.filters.model.clearAllFilterSelections
import com.woocommerce.android.ui.orders.filters.model.getNumberOfSelectedFilterOptions
import com.woocommerce.android.ui.orders.filters.model.isAnyFilterOptionSelected
import com.woocommerce.android.ui.orders.filters.model.markOptionAllIfNothingSelected
import com.woocommerce.android.ui.orders.filters.model.toOrderFilterOptionUiModel
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCCustomerStore
import javax.inject.Inject

@HiltViewModel
class OrderFilterCategoriesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val getOrderStatusFilterOptions: GetOrderStatusFilterOptions,
    private val getDateRangeFilterOptions: GetDateRangeFilterOptions,
    private val orderFilterRepository: OrderFiltersRepository,
    private val getTrackingForFilterSelection: GetTrackingForFilterSelection,
    private val dateUtils: DateUtils,
    private val analyticsTraWrapper: AnalyticsTrackerWrapper,
    private val productRepository: ProductListRepository,
    private val customerStore: WCCustomerStore,
    private val selectedSite: SelectedSite,
    private val isSalesChannelFilterSupported: IsSalesChannelFilterSupported,
    private val saveOrderFilterToHistory: SaveOrderFilterToHistory,
    private val orderFilterHistoryMapper: OrderFilterHistoryMapper,
    featureFlagRepository: FeatureFlagRepository
) : ScopedViewModel(savedState) {
    companion object {
        const val OLD_FILTER_SELECTION_KEY = "old_filter_selection_key"
        const val OLD_CUSTOM_DATE_RANGE_KEY = "old_custom_date_range_key"
    }

    val isFilterHistoryEnabled: Boolean = featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)

    private var oldFilterSelection: List<OrderFilterCategoryUiModel> =
        savedState[OLD_FILTER_SELECTION_KEY] ?: emptyList()

    // The custom date range days aren't captured in [oldFilterSelection] (which only holds option keys),
    // so snapshot them separately to restore the range when the user discards their changes.
    private var oldCustomDateRange: Pair<Long, Long> =
        savedState.get<LongArray>(OLD_CUSTOM_DATE_RANGE_KEY)?.let { it[0] to it[1] } ?: (0L to 0L)

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val categories = LiveDataDelegate(
        savedState,
        OrderFilterCategories(emptyList())
    )
    private var _categories by categories

    val orderFilterCategoryViewState = categories.liveData.map {
        getFilterCategoryViewState(it.list)
    }

    init {
        if (_categories.list.isEmpty()) {
            launch {
                _categories = OrderFilterCategories(buildFilterListUiModel())
                oldFilterSelection = _categories.list
                savedState[OLD_FILTER_SELECTION_KEY] = oldFilterSelection
                oldCustomDateRange = orderFilterRepository.getCustomDateRangeDays()
                savedState[OLD_CUSTOM_DATE_RANGE_KEY] =
                    longArrayOf(oldCustomDateRange.first, oldCustomDateRange.second)
            }
        }
    }

    fun onFilterCategorySelected(filterCategory: OrderFilterCategoryUiModel) {
        triggerEvent(ShowFilterOptionsForCategory(filterCategory))
    }

    fun onShowOrdersClicked() {
        saveFiltersSelection(_categories.list)
        trackFilterSelection()
        saveOrderFilterToHistory()
        triggerEvent(OnShowOrders)
    }

    fun onFilterHistoryButtonClicked() {
        analyticsTraWrapper.track(
            AnalyticsEvent.FILTER_HISTORY_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_FILTER_HISTORY_SOURCE_ORDERS)
        )
        triggerEvent(OpenFilterHistory)
    }

    fun onPastFilterSelected(savedFilter: SavedFilter) {
        val historyData = orderFilterHistoryMapper.fromPayload(savedFilter.payload) ?: return
        launch {
            // Reconcile the stored selection against what this store can currently show, so values that
            // are no longer available (e.g. the whole Sales Channel category on WooCommerce < 9.9, or a
            // deleted order status) aren't applied and left stuck active with no visible way to remove them.
            val availableCategories = buildFilterListUiModel()
            OrderListFilterCategory.entries.forEach { category ->
                val availableOptionKeys = availableCategories
                    .firstOrNull { it.categoryKey == category }
                    ?.orderFilterOptions
                    ?.map { it.key }
                    ?.toSet()
                val storedValues = historyData.selections[category.name] ?: emptyList()
                val reconciledValues = when {
                    // Category isn't available on this store, drop it entirely.
                    availableOptionKeys == null -> emptyList()
                    // Product/Customer options are derived from the value itself, so apply the stored value as-is.
                    category == PRODUCT || category == CUSTOMER -> storedValues
                    else -> storedValues.filter { it in availableOptionKeys }
                }
                orderFilterRepository.setSelectedFilters(category, reconciledValues)
            }
            orderFilterRepository.setCustomDateRange(historyData.customDateRangeStart, historyData.customDateRangeEnd)
            _categories = OrderFilterCategories(buildFilterListUiModel())
        }
    }

    fun onClearFilters() {
        analyticsTraWrapper.track(AnalyticsEvent.ORDER_FILTER_LIST_CLEAR_MENU_BUTTON_TAPPED)
        _categories = OrderFilterCategories(
            _categories.list.map {
                it.copy(
                    orderFilterOptions = it.orderFilterOptions
                        .clearAllFilterSelections()
                        .markOptionAllIfNothingSelected(),
                    displayValue = resourceProvider.getString(R.string.orderfilters_default_filter_value)
                )
            }
        )
        saveFiltersSelection(_categories.list)
    }

    fun onClearProductFilter() {
        _categories = _categories.copy(
            _categories.list.map {
                if (it.categoryKey == PRODUCT) {
                    it.copy(
                        orderFilterOptions = it.orderFilterOptions
                            .clearAllFilterSelections()
                            .markOptionAllIfNothingSelected(),
                        displayValue = resourceProvider.getString(R.string.orderfilters_default_filter_value)
                    )
                } else {
                    it
                }
            }
        )
    }

    fun onFilterOptionsUpdated(updatedCategory: OrderFilterCategoryUiModel) {
        _categories.list.let { filterOptions ->
            _categories = OrderFilterCategories(
                filterOptions.map {
                    if (it.categoryKey == updatedCategory.categoryKey) {
                        updateFilterOptionsForCategory(it, updatedCategory)
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun onBackPressed(): Boolean {
        return if (oldFilterSelection != _categories.list) {
            triggerEvent(
                ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = { _, _ ->
                        saveFiltersSelection(oldFilterSelection)
                        orderFilterRepository.setCustomDateRange(oldCustomDateRange.first, oldCustomDateRange.second)
                        triggerEvent(Exit)
                    },
                    negativeButtonId = R.string.keep_changes
                )
            )
            false
        } else {
            true
        }
    }

    private fun updateFilterOptionsForCategory(old: OrderFilterCategoryUiModel, new: OrderFilterCategoryUiModel) =
        old.copy(
            orderFilterOptions = new.orderFilterOptions,
            displayValue = new.orderFilterOptions.getDisplayValue(
                new.categoryKey,
                resourceProvider
            )
        )

    private suspend fun buildFilterListUiModel(): List<OrderFilterCategoryUiModel> {
        val orderStatusFilterUiOptions = loadOrderStatusFilterOptions()
        val dateRangeFilterOptions = loadDateRangeFilterOptions()
        val productFilterOptions = getProductFilterOptions()
        val customerFilterOptions = getCustomerFilterOptions()

        val baseFilters = listOf(
            OrderFilterCategoryUiModel(
                categoryKey = ORDER_STATUS,
                displayName = resourceProvider.getString(R.string.orderfilters_order_status_filter),
                displayValue = orderStatusFilterUiOptions.getDisplayValue(
                    ORDER_STATUS,
                    resourceProvider
                ),
                orderStatusFilterUiOptions
            ),
            OrderFilterCategoryUiModel(
                categoryKey = DATE_RANGE,
                displayName = resourceProvider.getString(R.string.orderfilters_date_range_filter),
                displayValue = dateRangeFilterOptions.getDisplayValue(
                    DATE_RANGE,
                    resourceProvider
                ),
                dateRangeFilterOptions
            ),
            OrderFilterCategoryUiModel(
                categoryKey = CUSTOMER,
                displayName = resourceProvider.getString(R.string.orderfilters_customer_filter),
                displayValue = customerFilterOptions.getDisplayValue(
                    PRODUCT,
                    resourceProvider
                ),
                orderFilterOptions = customerFilterOptions
            ),
            OrderFilterCategoryUiModel(
                categoryKey = PRODUCT,
                displayName = resourceProvider.getString(R.string.orderfilters_product_filter),
                displayValue = productFilterOptions.getDisplayValue(
                    PRODUCT,
                    resourceProvider
                ),
                productFilterOptions
            )
        )

        return if (isSalesChannelFilterSupported()) {
            val salesChannelFilterOptions = getSalesChannelFilterOptions()
            baseFilters + OrderFilterCategoryUiModel(
                categoryKey = SALES_CHANNEL,
                displayName = resourceProvider.getString(R.string.orderfilters_sales_channel_filter),
                displayValue = salesChannelFilterOptions.getDisplayValue(
                    SALES_CHANNEL,
                    resourceProvider
                ),
                salesChannelFilterOptions
            )
        } else {
            baseFilters
        }
    }

    private fun getProductFilterOptions(): List<OrderFilterOptionUiModel> {
        return listOfNotNull(
            orderFilterRepository.productFilter
                ?.let {
                    OrderFilterOptionUiModel(
                        key = it.toString(),
                        displayName = getProductDisplayValueFrom(it),
                        isSelected = true
                    )
                }
        )
    }

    private suspend fun getCustomerFilterOptions(): List<OrderFilterOptionUiModel> {
        return listOfNotNull(
            orderFilterRepository.customerFilter?.let {
                OrderFilterOptionUiModel(
                    key = it.toString(),
                    displayName = getCustomerDisplayValueFrom(it),
                    isSelected = true
                )
            }
        )
    }

    private fun loadDateRangeFilterOptions(): List<OrderFilterOptionUiModel> = getDateRangeFilterOptions()
        .map { it.toOrderFilterOptionUiModel(resourceProvider, dateUtils) }
        .toMutableList()
        .apply { addFilterOptionAll(resourceProvider) }

    private fun getFilterCategoryViewState(filterCategories: List<OrderFilterCategoryUiModel>):
        OrderFilterCategoryListViewState {
        val selectedFiltersCount = filterCategories
            .map { it.orderFilterOptions.getNumberOfSelectedFilterOptions() }
            .sum()

        val title = if (selectedFiltersCount > 0) {
            resourceProvider.getString(R.string.orderfilters_filters_count_title, selectedFiltersCount)
        } else {
            resourceProvider.getString(R.string.orderfilters_filters_default_title)
        }
        return OrderFilterCategoryListViewState(
            screenTitle = title,
            displayClearButton = selectedFiltersCount > 0
        )
    }

    private suspend fun loadOrderStatusFilterOptions(): List<OrderFilterOptionUiModel> = getOrderStatusFilterOptions()
        .map { it.toOrderFilterOptionUiModel(resourceProvider) }
        .toMutableList()
        .apply { addFilterOptionAll(resourceProvider) }

    private fun List<OrderFilterOptionUiModel>.getDisplayValue(
        selectedFilterCategoryKey: OrderListFilterCategory,
        resourceProvider: ResourceProvider
    ): String =
        if (isAnyFilterOptionSelected()) {
            when (selectedFilterCategoryKey) {
                ORDER_STATUS -> getNumberOfSelectedFilterOptions()
                    .toString()

                DATE_RANGE,
                PRODUCT,
                CUSTOMER,
                SALES_CHANNEL -> first { it.isSelected }.displayName
            }
        } else {
            resourceProvider.getString(R.string.orderfilters_default_filter_value)
        }

    private fun saveFiltersSelection(selectedFilters: List<OrderFilterCategoryUiModel>) {
        selectedFilters.forEach { category ->
            val newSelectedFilters = category.orderFilterOptions
                .filter { it.isSelected && it.key != OrderFilterOptionUiModel.DEFAULT_ALL_KEY }
                .map { it.key }
            orderFilterRepository.setSelectedFilters(category.categoryKey, newSelectedFilters)
        }
    }

    private fun trackFilterSelection() {
        if (_categories.list.isAnyFilterSelected()) {
            AnalyticsTracker.track(
                AnalyticsEvent.ORDERS_LIST_FILTER,
                getTrackingForFilterSelection()
            )
        }
    }

    private fun List<OrderFilterCategoryUiModel>.isAnyFilterSelected() =
        any { it.orderFilterOptions.isAnyFilterOptionSelected() }

    fun onCustomerSelected(customer: Order.Customer) {
        orderFilterRepository.customerFilter = customer.customerId
        launch {
            val displayValue = getCustomerDisplayValueFrom(customer.customerId)
            onFilterOptionsUpdated(
                OrderFilterCategoryUiModel(
                    categoryKey = CUSTOMER,
                    displayName = resourceProvider.getString(R.string.orderfilters_customer_filter),
                    displayValue = displayValue,
                    orderFilterOptions = listOf(
                        OrderFilterOptionUiModel(
                            key = customer.customerId?.toString() ?: error("Customer ID is null"),
                            displayName = displayValue,
                            isSelected = true
                        )
                    )
                )
            )
        }
    }

    fun onProductSelected(productId: Long) {
        orderFilterRepository.productFilter = productId
        onFilterOptionsUpdated(
            OrderFilterCategoryUiModel(
                categoryKey = PRODUCT,
                displayName = resourceProvider.getString(R.string.orderfilters_product_filter),
                displayValue = getProductDisplayValueFrom(productId),
                listOf(
                    OrderFilterOptionUiModel(
                        key = productId.toString(),
                        displayName = getProductDisplayValueFrom(productId),
                        isSelected = true
                    )
                )
            )
        )
    }

    private suspend fun getCustomerDisplayValueFrom(customerId: Long?): String =
        customerId?.let { id ->
            customerStore.getCustomerByRemoteId(selectedSite.get(), id)
                ?.let { customer ->
                    (customer.firstName + " " + customer.lastName)
                        .ifBlank { customer.email }
                        .ifBlank { customer.username }
                } ?: resourceProvider.getString(
                R.string.orderfilters_selected_filter_fallback_display_value,
                id
            )
        } ?: resourceProvider.getString(R.string.orderfilters_default_filter_value)

    private fun getProductDisplayValueFrom(productId: Long?): String =
        productId?.let { id ->
            productRepository.getProduct(id)?.name ?: resourceProvider.getString(
                R.string.orderfilters_selected_filter_fallback_display_value,
                id
            )
        } ?: resourceProvider.getString(R.string.orderfilters_default_filter_value)

    private fun getSalesChannelFilterOptions(): List<OrderFilterOptionUiModel> {
        val currentSelection = orderFilterRepository.getCurrentFilterSelection(SALES_CHANNEL)

        return mutableListOf(
            OrderFilterOptionUiModel(
                key = SalesChannel.POS.key,
                displayName = resourceProvider.getString(R.string.point_of_sale),
                isSelected = currentSelection.contains(SalesChannel.POS.key)
            ),
            OrderFilterOptionUiModel(
                key = SalesChannel.WEB_CHECKOUT.key,
                displayName = resourceProvider.getString(R.string.orderfilters_sales_channel_filter_web_checkout),
                isSelected = currentSelection.contains(SalesChannel.WEB_CHECKOUT.key)
            ),
            OrderFilterOptionUiModel(
                key = SalesChannel.WP_ADMIN.key,
                displayName = resourceProvider.getString(R.string.orderfilters_sales_channel_filter_wp_admin),
                isSelected = currentSelection.contains(SalesChannel.WP_ADMIN.key)
            )
        ).apply {
            addFilterOptionAll(resourceProvider)
        }
    }

    fun onSalesChannelSelected(selectedKey: String) {
        val salesChannelFilterOptions = when (selectedKey) {
            OrderFilterOptionUiModel.DEFAULT_ALL_KEY -> {
                getSalesChannelFilterOptions().map {
                    it.copy(
                        isSelected = it.key == OrderFilterOptionUiModel.DEFAULT_ALL_KEY
                    )
                }
            }
            else -> {
                getSalesChannelFilterOptions().map { it.copy(isSelected = it.key == selectedKey) }
            }
        }

        onFilterOptionsUpdated(
            OrderFilterCategoryUiModel(
                categoryKey = SALES_CHANNEL,
                displayName = resourceProvider.getString(R.string.orderfilters_sales_channel_filter),
                displayValue = salesChannelFilterOptions.getDisplayValue(
                    SALES_CHANNEL,
                    resourceProvider
                ),
                salesChannelFilterOptions
            )
        )
    }

    @Parcelize
    data class OrderFilterCategories(
        val list: List<OrderFilterCategoryUiModel>
    ) : Parcelable
}
