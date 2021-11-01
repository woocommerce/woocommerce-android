package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.clearAllFilterSelections
import com.woocommerce.android.ui.orders.filters.model.markOptionAllIfNothingSelected
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderFilterOptionListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val orderFilterRepository: OrderFiltersRepository
) : ScopedViewModel(savedState) {
    private val arguments: OrderFilterOptionListFragmentArgs by savedState.navArgs()

    private val _orderFilterOptionScreenTitle = MutableLiveData<String>()
    val orderFilterOptionScreenTitle: LiveData<String> = _orderFilterOptionScreenTitle

    private val _orderFilterOptions = MutableLiveData<List<OrderFilterOptionUiModel>>()
    val orderFilterOptions: LiveData<List<OrderFilterOptionUiModel>> = _orderFilterOptions

    private val selectedFilterCategory: OrderFilterCategoryUiModel = arguments.filterCategory

    init {
        _orderFilterOptions.value = selectedFilterCategory.orderFilterOptions
        _orderFilterOptionScreenTitle.value = getOrderFilterOptionsTitle(selectedFilterCategory)
    }

    fun onFilterOptionSelected(selectedOrderFilterOption: OrderFilterOptionUiModel) {
        when (selectedFilterCategory.categoryKey) {
            ORDER_STATUS -> updateOrderStatusSelectedFilters(selectedOrderFilterOption)
            DATE_RANGE -> updateDateRangeFilters(selectedOrderFilterOption)
        }
    }

    private fun updateOrderStatusSelectedFilters(orderStatusClicked: OrderFilterOptionUiModel) {
        when (orderStatusClicked.key) {
            OrderFilterOptionUiModel.DEFAULT_ALL_KEY -> _orderFilterOptions.value =
                _orderFilterOptions.value?.clearAllFilterSelections()
            else -> uncheckFilterOptionAll()
        }
        updateSelectedFilterValues(orderStatusClicked)
    }

    private fun updateDateRangeFilters(dateRangeOptionClicked: OrderFilterOptionUiModel) {
        _orderFilterOptions.value = _orderFilterOptions.value?.clearAllFilterSelections()
        updateSelectedFilterValues(dateRangeOptionClicked)
    }

    private fun updateSelectedFilterValues(selectedFilterOption: OrderFilterOptionUiModel) {
        _orderFilterOptions.value?.let { filterOptions ->
            var updatedOptionList = filterOptions.map {
                when (selectedFilterOption.key) {
                    it.key -> updateFilterOptionsSelectedValue(selectedFilterCategory, selectedFilterOption)
                    else -> it
                }
            }
            updatedOptionList = updatedOptionList.markOptionAllIfNothingSelected()
            _orderFilterOptions.value = updatedOptionList

        }
    }

    private fun updateFilterOptionsSelectedValue(
        selectedFilterCategory: OrderFilterCategoryUiModel?,
        filterOptionClicked: OrderFilterOptionUiModel
    ) =
        when {
            filterOptionClicked.key == OrderFilterOptionUiModel.DEFAULT_ALL_KEY ||
                selectedFilterCategory?.categoryKey == DATE_RANGE -> filterOptionClicked.copy(
                isSelected = true
            )
            else -> filterOptionClicked.copy(isSelected = !filterOptionClicked.isSelected)
        }

    private fun uncheckFilterOptionAll() {
        _orderFilterOptions.value = _orderFilterOptions.value
            ?.map {
                when (it.key) {
                    OrderFilterOptionUiModel.DEFAULT_ALL_KEY -> it.copy(isSelected = false)
                    else -> it
                }
            }
    }

    private fun getOrderFilterOptionsTitle(filterCategory: OrderFilterCategoryUiModel) =
        when (filterCategory.categoryKey) {
            ORDER_STATUS ->
                resourceProvider.getString(R.string.orderfilters_filter_order_status_options_title)
            DATE_RANGE ->
                resourceProvider.getString(R.string.orderfilters_filter_date_range_options_title)
        }

    fun onShowOrdersClicked() {
        val newSelectedFilters = orderFilterOptions.value
            ?.filter { it.isSelected }
            ?.map { it.key } ?: emptyList()
//        val filtersChanged = newSelectedFilters !=
//            orderFilterRepository.getCachedFiltersSelection()[selectedFilterCategory]
        orderFilterRepository.updateSelectedFilters(selectedFilterCategory.categoryKey, newSelectedFilters)
        triggerEvent(MultiLiveEvent.Event.ExitWithResult(true))
    }
}
