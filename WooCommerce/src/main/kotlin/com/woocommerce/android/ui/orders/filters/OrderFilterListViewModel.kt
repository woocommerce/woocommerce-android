package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterDateRangeUiModel.LAST_2_DAYS
import com.woocommerce.android.ui.orders.filters.model.OrderFilterDateRangeUiModel.THIS_MONTH
import com.woocommerce.android.ui.orders.filters.model.OrderFilterDateRangeUiModel.THIS_WEEK
import com.woocommerce.android.ui.orders.filters.model.OrderFilterDateRangeUiModel.TODAY
import com.woocommerce.android.ui.orders.filters.model.OrderFilterListEvent.ShowOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.model.OrderListFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderListFilterOptionUiModel.Companion.DEFAULT_ALL_KEY
import com.woocommerce.android.ui.orders.filters.model.clearAllFilterSelections
import com.woocommerce.android.ui.orders.filters.model.getDisplayValue
import com.woocommerce.android.ui.orders.filters.model.getNumberOfSelectedFilterOptions
import com.woocommerce.android.ui.orders.filters.model.isAnyFilterOptionSelected
import com.woocommerce.android.ui.orders.filters.model.markOptionAllIfNothingSelected
import com.woocommerce.android.ui.orders.filters.model.toFilterListOptionUiModel
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
class OrderFilterListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val orderListRepository: OrderListRepository,
    private val orderFilterRepository: OrderFiltersRepository
) : ScopedViewModel(savedState) {
    private val _orderFilterCategories = MutableLiveData<List<OrderFilterCategoryUiModel>>()
    val orderFilterCategories: LiveData<List<OrderFilterCategoryUiModel>> = _orderFilterCategories

    private val _orderFilterCategoryViewState = MutableLiveData<OrderFilterCategoryListViewState>()
    val orderFilterCategoryViewState: LiveData<OrderFilterCategoryListViewState> = _orderFilterCategoryViewState

    private var selectedFilterCategory: OrderFilterCategoryUiModel? = null

    init {
        launch {
            _orderFilterCategories.value = buildFilterListUiModel()
            _orderFilterCategoryViewState.value = getFilterCategoryViewState()
        }
    }

    private suspend fun buildFilterListUiModel(): List<OrderFilterCategoryUiModel> {
        val selectedFilters = orderFilterRepository.getCachedFiltersSelection()
        val orderStatusFilterOptions =
            loadOrderStatusFilterOptions(selectedFilters[OrderListFilterCategory.ORDER_STATUS])
        val dateRangeFilterOptions = loadDateRangeFilterOptions(selectedFilters[OrderListFilterCategory.DATE_RANGE])
        return listOf(
            OrderFilterCategoryUiModel(
                categoryKey = ORDER_STATUS,
                displayName = resourceProvider.getString(R.string.orderfilters_order_status_filter),
                displayValue = orderStatusFilterOptions.getDisplayValue(ORDER_STATUS, resourceProvider),
                orderStatusFilterOptions
            ),
            OrderFilterCategoryUiModel(
                categoryKey = DATE_RANGE,
                displayName = resourceProvider.getString(R.string.orderfilters_date_range_filter),
                displayValue = dateRangeFilterOptions.getDisplayValue(DATE_RANGE, resourceProvider),
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

    private suspend fun loadOrderStatusFilterOptions(selectedOrderStatusFilters: List<String>?):
        List<OrderListFilterOptionUiModel> {
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
        triggerEvent(ShowOrderStatusFilterOptions)
    }

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
