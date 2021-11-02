package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.filters.data.GetDateRangeFilterOptions
import com.woocommerce.android.ui.orders.filters.data.GetOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.ShowFilterOptionsForCategory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.addFilterOptionAll
import com.woocommerce.android.ui.orders.filters.model.clearAllFilterSelections
import com.woocommerce.android.ui.orders.filters.model.getNumberOfSelectedFilterOptions
import com.woocommerce.android.ui.orders.filters.model.isAnyFilterOptionSelected
import com.woocommerce.android.ui.orders.filters.model.markOptionAllIfNothingSelected
import com.woocommerce.android.ui.orders.filters.model.toOrderFilterOptionUiModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderFilterCategoriesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val getOrderStatusFilterOptions: GetOrderStatusFilterOptions,
    private val getDateRangeFilterOptions: GetDateRangeFilterOptions,
    private val orderFilterRepository: OrderFiltersRepository

) : ScopedViewModel(savedState) {
    companion object {
        const val KEY_SAVED_ORDER_STATUS_SELECTION = "key_current_order_status_selection"
        const val KEY_SAVED_DATE_RANGE_SELECTION = "key_current_date_range_selection"
    }

    private val _orderFilterCategories = MutableLiveData<List<OrderFilterCategoryUiModel>>()
    val orderFilterCategories: LiveData<List<OrderFilterCategoryUiModel>> = _orderFilterCategories

    private val _orderFilterCategoryViewState = MutableLiveData<OrderFilterCategoryListViewState>()
    val orderFilterCategoryViewState: LiveData<OrderFilterCategoryListViewState> = _orderFilterCategoryViewState

    init {
        launch {
            _orderFilterCategories.value = buildFilterListUiModel()
            _orderFilterCategoryViewState.value = getFilterCategoryViewState()
            saveUiState()
        }
    }

    fun onFilterCategorySelected(filterCategory: OrderFilterCategoryUiModel) {
        triggerEvent(ShowFilterOptionsForCategory(filterCategory))
    }

    fun onShowOrdersClicked() {
        saveFiltersSelection()
        triggerEvent(OnShowOrders)
    }

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

    fun onFilterOptionsUpdated(updatedCategory: OrderFilterCategoryUiModel) {
        _orderFilterCategories.value?.let { filterOptions ->
            _orderFilterCategories.value = filterOptions.map {
                if (it.categoryKey == updatedCategory.categoryKey) {
                    updateFilterOptionsForCategory(it, updatedCategory)
                } else it
            }
            _orderFilterCategoryViewState.value = getFilterCategoryViewState()
            saveUiState()
        }
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

    private fun updateFilterOptionsForCategory(old: OrderFilterCategoryUiModel, new: OrderFilterCategoryUiModel) =
        old.copy(
            orderFilterOptions = new.orderFilterOptions,
            displayValue = new.orderFilterOptions.getDisplayValue(
                new.categoryKey,
                resourceProvider
            )
        )

    private suspend fun buildFilterListUiModel(): List<OrderFilterCategoryUiModel> {
        val orderStatusFilterUiOptions =
            savedState.get<OrderFilterCategoryUiModel>(KEY_SAVED_ORDER_STATUS_SELECTION)?.orderFilterOptions
                ?: loadOrderStatusFilterOptions()
        val dateRangeFilterOptions =
            savedState.get<OrderFilterCategoryUiModel>(KEY_SAVED_DATE_RANGE_SELECTION)?.orderFilterOptions
                ?: loadDateRangeFilterOptions()

        return listOf(
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
            )
        )
    }

    private fun loadDateRangeFilterOptions(): List<OrderFilterOptionUiModel> = getDateRangeFilterOptions()
        .map { it.toOrderFilterOptionUiModel(resourceProvider) }
        .toMutableList()
        .apply { addFilterOptionAll(resourceProvider) }

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
                DATE_RANGE -> first { it.isSelected }.displayName
            }
        } else {
            resourceProvider.getString(R.string.orderfilters_default_filter_value)
        }

    private fun saveFiltersSelection() {
        _orderFilterCategories.value?.forEach { category ->
            val newSelectedFilters = category.orderFilterOptions
                .filter { it.isSelected && it.key != OrderFilterOptionUiModel.DEFAULT_ALL_KEY }
                .map { it.key }
            orderFilterRepository.updateSelectedFilters(category.categoryKey, newSelectedFilters)
        }
    }

    private fun saveUiState() {
        savedState[KEY_SAVED_ORDER_STATUS_SELECTION] = _orderFilterCategories.value
            ?.first { it.categoryKey == ORDER_STATUS }
        savedState[KEY_SAVED_DATE_RANGE_SELECTION] = _orderFilterCategories.value
            ?.first { it.categoryKey == DATE_RANGE }
    }
}
