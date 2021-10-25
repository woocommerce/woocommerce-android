package com.woocommerce.android.ui.orders.filters.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.ui.model.FilterListCategoryUiModel
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterListEvent.ShowOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.ui.model.OrderListFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.ui.model.OrderListFilterOptionUiModel.Companion.DEFAULT_ALL_KEY
import com.woocommerce.android.ui.orders.filters.ui.model.toFilterListOptionUiModel
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressWarnings("TooManyFunctions")
class OrderFilterListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val orderListRepository: OrderListRepository,
    private val orderFilterRepository: OrderFiltersRepository
) : ScopedViewModel(savedState) {
    private val _orderFilterCategories = MutableLiveData<List<FilterListCategoryUiModel>>()
    val orderFilterCategories: LiveData<List<FilterListCategoryUiModel>> = _orderFilterCategories

    private val _orderFilterOptions = MutableLiveData<List<OrderListFilterOptionUiModel>>()
    val orderOrderOptionsFilter: LiveData<List<OrderListFilterOptionUiModel>> = _orderFilterOptions

    private val _orderFilterCategoryViewState = MutableLiveData<OrderFilterCategoryListViewState>()
    val orderFilterCategoryViewState: LiveData<OrderFilterCategoryListViewState> = _orderFilterCategoryViewState

    private val _orderFilterOptionScreenTitle = MutableLiveData<String>()
    val orderFilterOptionScreenTitle: LiveData<String> = _orderFilterOptionScreenTitle

    private var selectedFilterCategory: FilterListCategoryUiModel? = null

    init {
        launch {
            _orderFilterCategories.value = buildFilterListUiModel()
            _orderFilterCategoryViewState.value = getFilterCategoryViewState()
        }
    }

    private suspend fun buildFilterListUiModel(): List<FilterListCategoryUiModel> {
        val currentOrderStatusFilterOptions = loadOrderStatusFilterOptions()
        return listOf(
            FilterListCategoryUiModel(
                categoryKey = ORDER_STATUS,
                displayName = resourceProvider.getString(R.string.orderfilters_order_status_filter),
                displayValue = getDisplayValueForSelectedOrderStatus(currentOrderStatusFilterOptions),
                currentOrderStatusFilterOptions
            ),
            FilterListCategoryUiModel(
                categoryKey = DATE_RANGE,
                displayName = resourceProvider.getString(R.string.orderfilters_date_range_filter),
                displayValue = resourceProvider.getString(R.string.orderfilters_default_filter_value),
                emptyList()
            )
        )
    }

    private fun getFilterCategoryViewState(): OrderFilterCategoryListViewState {
        val selectedOrderStatusFilters = orderFilterCategories.value
            ?.first { it.categoryKey == ORDER_STATUS }
            ?.orderFilterOptions?.getNumberOfSelectedFilterOptions() ?: 0

        val title = if (selectedOrderStatusFilters > 0) {
            resourceProvider.getString(R.string.orderfilters_filters_count_title, selectedOrderStatusFilters)
        } else {
            resourceProvider.getString(R.string.orderfilters_filters_default_title)
        }
        return OrderFilterCategoryListViewState(
            screenTitle = title,
            displayClearButton = selectedOrderStatusFilters > 0
        )
    }

    private fun getDisplayValueForSelectedOrderStatus(orderStatusOrderFilters: List<OrderListFilterOptionUiModel>): String =
        if (orderStatusOrderFilters.isAnyOrderStatusSelected()) {
            orderStatusOrderFilters.getNumberOfSelectedFilterOptions().toString()
        } else {
            resourceProvider.getString(R.string.orderfilters_default_filter_value)
        }

    private suspend fun loadOrderStatusFilterOptions(): List<OrderListFilterOptionUiModel> {
        var orderStatus = orderListRepository.getCachedOrderStatusOptions()
        if (orderStatus.isEmpty()) {
            when (orderListRepository.fetchOrderStatusOptionsFromApi()) {
                RequestResult.SUCCESS -> orderStatus = orderListRepository.getCachedOrderStatusOptions()
                else -> { /* do nothing */
                }
            }
        }
        return orderStatus.values
            .map { it.toFilterListOptionUiModel(resourceProvider) }
            .toMutableList()
            .apply { add(index = 0, buildFilterOptionAll()) }
    }

    fun onFilterCategoryClicked(filterCategory: FilterListCategoryUiModel) {
        selectedFilterCategory = filterCategory
        _orderFilterOptions.value = filterCategory.orderFilterOptions
        _orderFilterOptionScreenTitle.value = getOrderFilterOptionsTitle(filterCategory)
        triggerEvent(ShowOrderStatusFilterOptions)
    }

    private fun getOrderFilterOptionsTitle(filterCategory: FilterListCategoryUiModel) =
        when (filterCategory.categoryKey) {
            ORDER_STATUS ->
                resourceProvider.getString(R.string.orderfilters_filter_order_status_options_title)
            DATE_RANGE ->
                resourceProvider.getString(R.string.orderfilters_filter_date_range_options_title)
        }

    fun onFilterOptionClicked(selectedOrderFilterOption: OrderListFilterOptionUiModel) {
        when (selectedFilterCategory?.categoryKey) {
            ORDER_STATUS -> updateOrderStatusSelectedFilters(selectedOrderFilterOption)
            DATE_RANGE -> {
            }
        }
    }

    private fun updateOrderStatusSelectedFilters(orderStatusClicked: OrderListFilterOptionUiModel) {
        when (orderStatusClicked.key) {
            DEFAULT_ALL_KEY -> _orderFilterOptions.value = orderOrderOptionsFilter.value?.clearAllFilterSelections()
            else -> uncheckFilterOptionAll()
        }

        _orderFilterOptions.value?.let { filterOptions ->
            val updatedOptionList = filterOptions.map {
                when (orderStatusClicked.displayName) {
                    it.displayName -> updateOrderStatusSelectedValue(orderStatusClicked)
                    else -> it
                }
            }
            _orderFilterOptions.value = updatedOptionList

            _orderFilterCategories.value?.let { filterCategories ->
                val updatedFilters = filterCategories.map { category ->
                    when (category.categoryKey) {
                        ORDER_STATUS -> updateFilterSelectedCategory(
                            category,
                            updatedOptionList
                        )
                        DATE_RANGE -> category
                    }
                }
                _orderFilterCategories.value = updatedFilters
            }
        }
        _orderFilterCategoryViewState.value = getFilterCategoryViewState()
    }

    private fun updateOrderStatusSelectedValue(orderStatusClicked: OrderListFilterOptionUiModel) =
        when (orderStatusClicked.key) {
            DEFAULT_ALL_KEY -> orderStatusClicked.copy(isSelected = true)
            else -> orderStatusClicked.copy(isSelected = !orderStatusClicked.isSelected)
        }

    private fun uncheckFilterOptionAll() {
        _orderFilterOptions.value = _orderFilterOptions.value
            ?.map {
                when (it.key) {
                    DEFAULT_ALL_KEY -> it.copy(isSelected = false)
                    else -> it
                }
            }
    }

    private fun updateFilterSelectedCategory(
        filterCategory: FilterListCategoryUiModel,
        updatedOptionListFilter: List<OrderListFilterOptionUiModel>
    ) = filterCategory.copy(
        displayValue = getDisplayValueForSelectedOrderStatus(updatedOptionListFilter),
        orderFilterOptions = updatedOptionListFilter
    )

    fun onShowOrdersClicked() {
        val selectedFiltersMap = orderFilterCategories.value
            ?.map { it.categoryKey to selectedFilterOptionKeys(it.orderFilterOptions) }
            ?.toMap()
            ?.toMutableMap() ?: mutableMapOf()
        val filtersChanged = selectedFiltersMap != orderFilterRepository.getCachedSelectedFilters()
        orderFilterRepository.updateSelectedFilters(selectedFiltersMap)
        triggerEvent(ExitWithResult(filtersChanged))
    }

    private fun selectedFilterOptionKeys(filterOptions: List<OrderListFilterOptionUiModel>) =
        filterOptions.filter { it.isSelected }.map { it.key }

    fun onClearFilterSelected() {
        updateOrderStatusSelectedFilters(buildFilterOptionAll())
    }

    private fun buildFilterOptionAll() = OrderListFilterOptionUiModel(
        key = DEFAULT_ALL_KEY,
        displayName = resourceProvider.getString(R.string.orderfilters_default_filter_value),
        isSelected = true
    )

    private fun List<OrderListFilterOptionUiModel>.getNumberOfSelectedFilterOptions() =
        filter { it.isSelected && it.key != DEFAULT_ALL_KEY }.count()

    private fun List<OrderListFilterOptionUiModel>.isAnyOrderStatusSelected() =
        any { it.isSelected && it.key != DEFAULT_ALL_KEY }

    private fun List<OrderListFilterOptionUiModel>.clearAllFilterSelections() =
        map { it.copy(isSelected = false) }

    fun onBackPressed(): Boolean {
        val selectedFiltersCount = _orderFilterCategories.value
            ?.map { it.orderFilterOptions.getNumberOfSelectedFilterOptions() }
            ?.sum() ?: 0
        return if (selectedFiltersCount > 0) {
            triggerEvent(
                ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = { _, _ ->
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
}
