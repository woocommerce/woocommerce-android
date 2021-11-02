package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnFilterOptionsSelectionUpdated
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel.Companion.DEFAULT_ALL_KEY
import com.woocommerce.android.ui.orders.filters.model.clearAllFilterSelections
import com.woocommerce.android.ui.orders.filters.model.markOptionAllIfNothingSelected
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderFilterOptionsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val orderFilterRepository: OrderFiltersRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_SELECTED_CATEGORY = "key_selected_category"
    }

    private val arguments: OrderFilterOptionsFragmentArgs by savedState.navArgs()

    private val _orderFilterOptionScreenTitle = MutableLiveData<String>()
    val orderFilterOptionScreenTitle: LiveData<String> = _orderFilterOptionScreenTitle

    private val _orderFilterOptions = MutableLiveData<List<OrderFilterOptionUiModel>>()
    val orderFilterOptions: LiveData<List<OrderFilterOptionUiModel>> = _orderFilterOptions

    private var selectedFilterCategory: OrderFilterCategoryUiModel =
        savedState.get<OrderFilterCategoryUiModel>(KEY_SELECTED_CATEGORY) ?: arguments.filterCategory

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

    fun onShowOrdersClicked() {
        saveFiltersSelection()
        triggerEvent(OnShowOrders)
    }

    private fun saveFiltersSelection() {
        val newSelectedFilters = orderFilterOptions.value
            ?.filter { it.isSelected && it.key != DEFAULT_ALL_KEY }
            ?.map { it.key } ?: emptyList()
        orderFilterRepository.updateSelectedFilters(selectedFilterCategory.categoryKey, newSelectedFilters)
    }

    fun onBackPressed(): Boolean {
        saveFiltersSelection()
        val updatedCategory = selectedFilterCategory.copy(
            orderFilterOptions = orderFilterOptions.value ?: emptyList()
        )
        triggerEvent(OnFilterOptionsSelectionUpdated(updatedCategory))
        return false
    }

    private fun updateOrderStatusSelectedFilters(orderStatusClicked: OrderFilterOptionUiModel) {
        when (orderStatusClicked.key) {
            DEFAULT_ALL_KEY -> _orderFilterOptions.value =
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
            savedState[KEY_SELECTED_CATEGORY] = selectedFilterCategory.copy(
                orderFilterOptions = updatedOptionList
            )
        }
    }

    private fun updateFilterOptionsSelectedValue(
        selectedFilterCategory: OrderFilterCategoryUiModel?,
        filterOptionClicked: OrderFilterOptionUiModel
    ) =
        when {
            filterOptionClicked.key == DEFAULT_ALL_KEY ||
                selectedFilterCategory?.categoryKey == DATE_RANGE ->
                filterOptionClicked.copy(isSelected = true)
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

    private fun getOrderFilterOptionsTitle(filterCategory: OrderFilterCategoryUiModel) =
        when (filterCategory.categoryKey) {
            ORDER_STATUS ->
                resourceProvider.getString(R.string.orderfilters_filter_order_status_options_title)
            DATE_RANGE ->
                resourceProvider.getString(R.string.orderfilters_filter_date_range_options_title)
        }
}
