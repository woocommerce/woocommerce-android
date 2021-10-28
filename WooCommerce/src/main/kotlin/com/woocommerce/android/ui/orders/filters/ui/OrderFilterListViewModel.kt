package com.woocommerce.android.ui.orders.filters.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterDateRange.LAST_2_DAYS
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterDateRange.THIS_MONTH
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterDateRange.THIS_WEEK
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterDateRange.TODAY
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
    private val _orderFilterCategories = MutableLiveData<List<OrderFilterCategoryUiModel>>()
    val orderFilterCategories: LiveData<List<OrderFilterCategoryUiModel>> = _orderFilterCategories

    private val _orderFilterOptions = MutableLiveData<List<OrderListFilterOptionUiModel>>()
    val orderOptionsFilter: LiveData<List<OrderListFilterOptionUiModel>> = _orderFilterOptions

    private val _orderFilterCategoryViewState = MutableLiveData<OrderFilterCategoryListViewState>()
    val orderFilterCategoryViewState: LiveData<OrderFilterCategoryListViewState> = _orderFilterCategoryViewState

    private val _orderFilterOptionScreenTitle = MutableLiveData<String>()
    val orderFilterOptionScreenTitle: LiveData<String> = _orderFilterOptionScreenTitle

    private var selectedFilterCategory: OrderFilterCategoryUiModel? = null

    init {
        launch {
            _orderFilterCategories.value = buildFilterListUiModel()
            _orderFilterCategoryViewState.value = getFilterCategoryViewState()
        }
    }

    private suspend fun buildFilterListUiModel(): List<OrderFilterCategoryUiModel> {
        val selectedFilters = orderFilterRepository.getCachedFiltersSelection()
        val orderStatusFilterOptions = loadOrderStatusFilterOptions(selectedFilters[ORDER_STATUS])
        val dateRangeFilterOptions = loadDateRangeFilterOptions(selectedFilters[DATE_RANGE])
        return listOf(
            OrderFilterCategoryUiModel(
                categoryKey = ORDER_STATUS,
                displayName = resourceProvider.getString(R.string.orderfilters_order_status_filter),
                displayValue = getDisplayValueForSelectedFilters(ORDER_STATUS, orderStatusFilterOptions),
                orderStatusFilterOptions
            ),
            OrderFilterCategoryUiModel(
                categoryKey = DATE_RANGE,
                displayName = resourceProvider.getString(R.string.orderfilters_date_range_filter),
                displayValue = getDisplayValueForSelectedFilters(DATE_RANGE, dateRangeFilterOptions),
                dateRangeFilterOptions
            )
        )
    }

    private fun loadDateRangeFilterOptions(selectedDateRangeFilter: List<String>?): List<OrderListFilterOptionUiModel> =
        listOf(TODAY, LAST_2_DAYS, THIS_WEEK, THIS_MONTH)
            .map { it.toFilterListOptionUiModel(resourceProvider) }
            .map { it.markAsSelectedIfPreviouslySelected(selectedDateRangeFilter) }
            .toMutableList()
            .apply { addFilterOptionAll() }

    private fun getFilterCategoryViewState(): OrderFilterCategoryListViewState {
        val selectedFiltersCount = orderFilterCategories.value
            ?.map { it.orderFilterOptions.getNumberOfSelectedFilterOptions() }
            ?.sum() ?: 0

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

    private fun getDisplayValueForSelectedFilters(
        selectedFilterCategoryKey: OrderListFilterCategory,
        selectedFilterOptions: List<OrderListFilterOptionUiModel>
    ): String =
        if (selectedFilterOptions.isAnyFilterOptionSelected()) {
            when (selectedFilterCategoryKey) {
                ORDER_STATUS -> selectedFilterOptions.getNumberOfSelectedFilterOptions().toString()
                DATE_RANGE -> selectedFilterOptions.first { it.isSelected }.displayName
            }
        } else {
            resourceProvider.getString(R.string.orderfilters_default_filter_value)
        }

    private suspend fun loadOrderStatusFilterOptions(selectedOrderStatusFilters: List<String>?): List<OrderListFilterOptionUiModel> {
        var orderStatus = orderListRepository.getCachedOrderStatusOptions()
        if (orderStatus.isEmpty()) {
            when (orderListRepository.fetchOrderStatusOptionsFromApi()) {
                SUCCESS -> orderStatus = orderListRepository.getCachedOrderStatusOptions()
                else -> { /* do nothing */
                }
            }
        }
        return orderStatus.values
            .map { it.toFilterListOptionUiModel(resourceProvider) }
            .map { it.markAsSelectedIfPreviouslySelected(selectedOrderStatusFilters) }
            .toMutableList()
            .apply { addFilterOptionAll() }
    }

    private fun MutableList<OrderListFilterOptionUiModel>.addFilterOptionAll() {
        add(
            index = 0,
            OrderListFilterOptionUiModel(
                key = DEFAULT_ALL_KEY,
                displayName = resourceProvider.getString(R.string.orderfilters_default_filter_value),
                isSelected = !isAnyFilterOptionSelected()
            )
        )
    }

    private fun OrderListFilterOptionUiModel.markAsSelectedIfPreviouslySelected(
        selectedOrderStatusFilters: List<String>?
    ) = copy(isSelected = selectedOrderStatusFilters?.contains(key) ?: false)

    fun onFilterCategorySelected(filterCategory: OrderFilterCategoryUiModel) {
        selectedFilterCategory = filterCategory
        _orderFilterOptions.value = filterCategory.orderFilterOptions
        _orderFilterOptionScreenTitle.value = getOrderFilterOptionsTitle(filterCategory)
        triggerEvent(ShowOrderStatusFilterOptions)
    }

    private fun getOrderFilterOptionsTitle(filterCategory: OrderFilterCategoryUiModel) =
        when (filterCategory.categoryKey) {
            ORDER_STATUS ->
                resourceProvider.getString(R.string.orderfilters_filter_order_status_options_title)
            DATE_RANGE ->
                resourceProvider.getString(R.string.orderfilters_filter_date_range_options_title)
        }

    fun onFilterOptionSelected(selectedOrderFilterOption: OrderListFilterOptionUiModel) {
        when (selectedFilterCategory?.categoryKey) {
            ORDER_STATUS -> updateOrderStatusSelectedFilters(selectedOrderFilterOption)
            DATE_RANGE -> updateDateRangeFilters(selectedOrderFilterOption)
        }
    }

    private fun updateDateRangeFilters(dateRangeOptionClicked: OrderListFilterOptionUiModel) {
        _orderFilterOptions.value = _orderFilterOptions.value?.clearAllFilterSelections()
        updateSelectedFilterValues(dateRangeOptionClicked)
        _orderFilterCategoryViewState.value = getFilterCategoryViewState()
    }

    private fun updateOrderStatusSelectedFilters(orderStatusClicked: OrderListFilterOptionUiModel) {
        when (orderStatusClicked.key) {
            DEFAULT_ALL_KEY -> _orderFilterOptions.value = _orderFilterOptions.value?.clearAllFilterSelections()
            else -> uncheckFilterOptionAll()
        }
        updateSelectedFilterValues(orderStatusClicked)
        _orderFilterCategoryViewState.value = getFilterCategoryViewState()
    }

    private fun updateSelectedFilterValues(selectedFilterOption: OrderListFilterOptionUiModel) {
        _orderFilterOptions.value?.let { filterOptions ->
            var updatedOptionList = filterOptions.map {
                when (selectedFilterOption.key) {
                    it.key -> updateFilterOptionsSelectedValue(selectedFilterCategory, selectedFilterOption)
                    else -> it
                }
            }
            updatedOptionList = updatedOptionList.markOptionAllIfNothingSelected()
            _orderFilterOptions.value = updatedOptionList

            _orderFilterCategories.value?.let { filterCategories ->
                val updatedFilters = filterCategories
                    .map {
                        when (selectedFilterCategory?.categoryKey) {
                            it.categoryKey -> updateFilterOptionsForCategory(it, updatedOptionList)
                            else -> it
                        }
                    }
                _orderFilterCategories.value = updatedFilters
            }
        }
    }

    private fun List<OrderListFilterOptionUiModel>.markOptionAllIfNothingSelected() =
        if (!isAnyFilterOptionSelected()) {
            map {
                when (it.key) {
                    DEFAULT_ALL_KEY -> it.copy(isSelected = true)
                    else -> it
                }
            }
        } else this

    private fun updateFilterOptionsSelectedValue(
        selectedFilterCategory: OrderFilterCategoryUiModel?,
        filterOptionClicked: OrderListFilterOptionUiModel
    ) =
        when {
            filterOptionClicked.key == DEFAULT_ALL_KEY ||
                selectedFilterCategory?.categoryKey == DATE_RANGE -> filterOptionClicked.copy(isSelected = true)
            else -> filterOptionClicked.copy(isSelected = !filterOptionClicked.isSelected)
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

    private fun updateFilterOptionsForCategory(
        filterCategory: OrderFilterCategoryUiModel,
        updatedOptionListFilter: List<OrderListFilterOptionUiModel>
    ) = filterCategory.copy(
        displayValue = getDisplayValueForSelectedFilters(filterCategory.categoryKey, updatedOptionListFilter),
        orderFilterOptions = updatedOptionListFilter
    )

    fun onShowOrdersClicked() {
        val filtersSelectionMap = orderFilterCategories.value
            ?.filter { it.orderFilterOptions.isAnyFilterOptionSelected() }
            ?.map { it.categoryKey to it.orderFilterOptions.selectedFilterOptionKeys() }
            ?.toMap() ?: mutableMapOf()
        val filtersChanged = filtersSelectionMap != orderFilterRepository.getCachedFiltersSelection()
        orderFilterRepository.updateSelectedFilters(filtersSelectionMap)
        triggerEvent(ExitWithResult(filtersChanged))
    }

    private fun List<OrderListFilterOptionUiModel>.selectedFilterOptionKeys() =
        filter { it.isSelected }
            .map { it.key }

    fun onClearFilters() {
        _orderFilterCategories.value = _orderFilterCategories.value
            ?.map {
                it.copy(
                    orderFilterOptions = it.orderFilterOptions
                        .clearAllFilterSelections()
                        .markOptionAllIfNothingSelected(),
                    displayValue = resourceProvider.getString(R.string.orderfilters_default_filter_value)
                )
            }
        _orderFilterCategoryViewState.value = getFilterCategoryViewState()
    }

    private fun List<OrderListFilterOptionUiModel>.getNumberOfSelectedFilterOptions() =
        filter { it.isSelected && it.key != DEFAULT_ALL_KEY }.count()

    private fun List<OrderListFilterOptionUiModel>.isAnyFilterOptionSelected() =
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
