package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderListFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.clearAllFilterSelections
import com.woocommerce.android.ui.orders.filters.model.getDisplayValue
import com.woocommerce.android.ui.orders.filters.model.markOptionAllIfNothingSelected
import com.woocommerce.android.ui.products.ProductFilterListFragmentArgs
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import javax.inject.Inject

class OrderFilterOptionListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val orderFilterRepository: OrderFiltersRepository
) : ScopedViewModel(savedState) {
    private val arguments: ProductFilterListFragmentArgs by savedState.navArgs()

    private val _orderFilterOptionScreenTitle = MutableLiveData<String>()
    val orderFilterOptionScreenTitle: LiveData<String> = _orderFilterOptionScreenTitle

    private val _orderFilterOptions = MutableLiveData<List<OrderListFilterOptionUiModel>>()
    val orderFilterOptions: LiveData<List<OrderListFilterOptionUiModel>> = _orderFilterOptions

    private lateinit var selectedFilterCategory: OrderFilterCategoryUiModel

    init {

    }

    fun onFilterOptionSelected(selectedOrderFilterOption: OrderListFilterOptionUiModel) {
        when (selectedFilterCategory?.categoryKey) {
            OrderFiltersRepository.OrderListFilterCategory.ORDER_STATUS -> updateOrderStatusSelectedFilters(
                selectedOrderFilterOption
            )
            OrderFiltersRepository.OrderListFilterCategory.DATE_RANGE -> updateDateRangeFilters(
                selectedOrderFilterOption
            )
        }
    }

    private fun updateOrderStatusSelectedFilters(orderStatusClicked: OrderListFilterOptionUiModel) {
        when (orderStatusClicked.key) {
            OrderListFilterOptionUiModel.DEFAULT_ALL_KEY -> _orderFilterOptions.value =
                _orderFilterOptions.value?.clearAllFilterSelections()
            else -> uncheckFilterOptionAll()
        }
        updateSelectedFilterValues(orderStatusClicked)
    }

    private fun updateDateRangeFilters(dateRangeOptionClicked: OrderListFilterOptionUiModel) {
        _orderFilterOptions.value = _orderFilterOptions.value?.clearAllFilterSelections()
        updateSelectedFilterValues(dateRangeOptionClicked)
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

        }
    }

    private fun updateFilterOptionsSelectedValue(
        selectedFilterCategory: OrderFilterCategoryUiModel?,
        filterOptionClicked: OrderListFilterOptionUiModel
    ) =
        when {
            filterOptionClicked.key == OrderListFilterOptionUiModel.DEFAULT_ALL_KEY ||
                selectedFilterCategory?.categoryKey == OrderFiltersRepository.OrderListFilterCategory.DATE_RANGE -> filterOptionClicked.copy(
                isSelected = true
            )
            else -> filterOptionClicked.copy(isSelected = !filterOptionClicked.isSelected)
        }

    private fun uncheckFilterOptionAll() {
        _orderFilterOptions.value = _orderFilterOptions.value
            ?.map {
                when (it.key) {
                    OrderListFilterOptionUiModel.DEFAULT_ALL_KEY -> it.copy(isSelected = false)
                    else -> it
                }
            }
    }

    private fun updateFilterOptionsForCategory(
        filterCategory: OrderFilterCategoryUiModel,
        updatedOptionListFilter: List<OrderListFilterOptionUiModel>
    ) = filterCategory.copy(
        displayValue = updatedOptionListFilter.getDisplayValue(filterCategory.categoryKey, resourceProvider),
        orderFilterOptions = updatedOptionListFilter
    )

    private fun getOrderFilterOptionsTitle(filterCategory: OrderFilterCategoryUiModel) =
        when (filterCategory.categoryKey) {
            OrderFiltersRepository.OrderListFilterCategory.ORDER_STATUS ->
                resourceProvider.getString(R.string.orderfilters_filter_order_status_options_title)
            OrderFiltersRepository.OrderListFilterCategory.DATE_RANGE ->
                resourceProvider.getString(R.string.orderfilters_filter_date_range_options_title)
        }

    fun onShowOrdersClicked() {
        val newSelectedFilters = orderFilterOptions.value
            ?.filter { it.isSelected }
            ?.map { it.key } ?: emptyList()

        val filtersChanged = newSelectedFilters !=
            orderFilterRepository.getCachedFiltersSelection()[selectedFilterCategory]
        orderFilterRepository.updateSelectedFilters(selectedFilterCategory.categoryKey, newSelectedFilters)
        triggerEvent(MultiLiveEvent.Event.ExitWithResult(filtersChanged))
    }

}
