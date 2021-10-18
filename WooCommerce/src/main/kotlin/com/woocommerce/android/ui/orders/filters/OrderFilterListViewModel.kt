package com.woocommerce.android.ui.orders.filters

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListCategoryUiModel.DateRangeFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListCategoryUiModel.OrderStatusFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.OrderFilterListEvent.ShowOrderStatusFilterOptions
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderFilterListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedState) {

    private val _orderFilterCategories = MutableLiveData<List<FilterListCategoryUiModel>>()
    val orderFilterCategories: LiveData<List<FilterListCategoryUiModel>> = _orderFilterCategories

    private val _orderFilterOptions = MutableLiveData<List<FilterListOptionUiModel>>()
    val orderFilterOptions: LiveData<List<FilterListOptionUiModel>> = _orderFilterOptions

    private val _orderFilterCategoryViewState = MutableLiveData<OrderFilterCategoryListViewState>()
    val orderFilterCategoryViewState: LiveData<OrderFilterCategoryListViewState> = _orderFilterCategoryViewState

    private val _orderFilterOptionScreenTitle = MutableLiveData<String>()
    val orderFilterOptionScreenTitle: LiveData<String> = _orderFilterOptionScreenTitle

    private var selectedFilterCategory: FilterListCategoryUiModel? = null

    init {
        _orderFilterCategories.value = buildFilterListUiModel()
        _orderFilterCategoryViewState.value = getFilterCategoryViewState()
    }

    private fun buildFilterListUiModel(): List<FilterListCategoryUiModel> {
        val currentOrderStatusFilterOptions = loadOrderStatusFilterOptions()
        return listOf(
            OrderStatusFilterCategoryUiModel(
                displayName = resourceProvider.getString(R.string.orderfilters_order_status_filter),
                displayValue = getDisplayValueForSelectedOrderStatus(currentOrderStatusFilterOptions),
                currentOrderStatusFilterOptions
            ),
            DateRangeFilterCategoryUiModel(
                displayName = resourceProvider.getString(R.string.orderfilters_date_range_filter),
                displayValue = resourceProvider.getString(R.string.orderfilters_default_filter_value),
                emptyList()
            )
        )
    }

    private fun getFilterCategoryViewState(): OrderFilterCategoryListViewState {
        val selectedOrderStatusFilters = orderFilterCategories.value
            ?.first { it is OrderStatusFilterCategoryUiModel }
            ?.filterOptions?.getNumberOfOrderStatusSelected() ?: 0

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

    private fun getDisplayValueForSelectedOrderStatus(orderStatusFilters: List<FilterListOptionUiModel>): String =
        if (orderStatusFilters.isAnyOrderStatusSelected()) {
            orderStatusFilters.getNumberOfOrderStatusSelected().toString()
        } else {
            resourceProvider.getString(R.string.orderfilters_default_filter_value)
        }

    private fun loadOrderStatusFilterOptions(): List<FilterListOptionUiModel> {
        return listOf(
            FilterListOptionUiModel("All", isSelected = true),
            FilterListOptionUiModel("Cancelled"),
            FilterListOptionUiModel("Completed"),
            FilterListOptionUiModel("Failed"),
            FilterListOptionUiModel("On hold"),
        )
    }

    fun onFilterCategoryClicked(filterCategory: FilterListCategoryUiModel) {
        selectedFilterCategory = filterCategory
        _orderFilterOptions.value = filterCategory.filterOptions
        _orderFilterOptionScreenTitle.value = getOrderFilterOptionsTitle(filterCategory)
        triggerEvent(ShowOrderStatusFilterOptions)
    }

    private fun getOrderFilterOptionsTitle(filterCategory: FilterListCategoryUiModel) =
        when (filterCategory) {
            is OrderStatusFilterCategoryUiModel ->
                resourceProvider.getString(R.string.orderfilters_filter_order_status_options_title)
            is DateRangeFilterCategoryUiModel ->
                resourceProvider.getString(R.string.orderfilters_filter_date_range_options_title)
        }

    fun onFilterOptionClicked(selectedFilterOption: FilterListOptionUiModel) {
        when (selectedFilterCategory) {
            is OrderStatusFilterCategoryUiModel -> updateOrderStatusSelectedFilters(selectedFilterOption)
            is DateRangeFilterCategoryUiModel -> {
            }
        }
    }

    private fun updateOrderStatusSelectedFilters(orderStatusClicked: FilterListOptionUiModel) {
        if (isOptionAllClicked(orderStatusClicked)) {
            _orderFilterOptions.value = orderFilterOptions.value?.clearAllFilterSelections()
        } else {
            uncheckOptionAll()
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
                    when (category) {
                        is OrderStatusFilterCategoryUiModel -> updateFilterSelectedCategory(
                            category,
                            updatedOptionList
                        )
                        is DateRangeFilterCategoryUiModel -> category
                    }
                }
                _orderFilterCategories.value = updatedFilters
            }
        }
        _orderFilterCategoryViewState.value = getFilterCategoryViewState()
    }

    private fun updateOrderStatusSelectedValue(orderStatusClicked: FilterListOptionUiModel) =
        when {
            isOptionAllClicked(orderStatusClicked) -> orderStatusClicked.copy(isSelected = true)
            else -> orderStatusClicked.copy(isSelected = !orderStatusClicked.isSelected)
        }

    private fun uncheckOptionAll() {
        _orderFilterOptions.value = _orderFilterOptions.value
            ?.mapIndexed { index, orderStatus ->
                if (index == 0) orderStatus.copy(isSelected = false)
                else orderStatus
            }
    }

    private fun isOptionAllClicked(selectedOrderStatusFilter: FilterListOptionUiModel) =
        selectedOrderStatusFilter.displayName == resourceProvider.getString(R.string.orderfilters_default_filter_value)

    private fun updateFilterSelectedCategory(
        filterCategory: OrderStatusFilterCategoryUiModel,
        updatedOptionList: List<FilterListOptionUiModel>
    ) = filterCategory.copy(
        displayValue = getDisplayValueForSelectedOrderStatus(updatedOptionList),
        filterOptions = updatedOptionList
    )

    fun onShowOrdersClicked() {
        TODO("Not yet implemented")
    }

    fun onClearFilterSelected() {
        selectOptionAllForOrderStatusFilter()
    }

    private fun selectOptionAllForOrderStatusFilter() {
        updateOrderStatusSelectedFilters(
            FilterListOptionUiModel(
                displayName = resourceProvider.getString(R.string.orderfilters_default_filter_value),
                isSelected = true
            )
        )
    }

    private fun List<FilterListOptionUiModel>.getNumberOfOrderStatusSelected() =
        drop(1)
            .filter { it.isSelected }
            .count()

    private fun List<FilterListOptionUiModel>.isAnyOrderStatusSelected() =
        drop(1)
            .any { it.isSelected }

    private fun List<FilterListOptionUiModel>.clearAllFilterSelections() =
        map { it.copy(isSelected = false) }

    sealed class OrderFilterListEvent : MultiLiveEvent.Event() {
        object ShowOrderStatusFilterOptions : OrderFilterListEvent()
    }

    @Parcelize
    data class OrderFilterCategoryListViewState(
        val screenTitle: String,
        val displayClearButton: Boolean = false
    ) : Parcelable

    sealed class FilterListCategoryUiModel : Parcelable {
        abstract val displayName: String
        abstract val displayValue: String
        abstract val filterOptions: List<FilterListOptionUiModel>

        @Parcelize
        data class OrderStatusFilterCategoryUiModel(
            override val displayName: String,
            override val displayValue: String,
            override val filterOptions: List<FilterListOptionUiModel>
        ) : Parcelable, FilterListCategoryUiModel()

        @Parcelize
        data class DateRangeFilterCategoryUiModel(
            override val displayName: String,
            override val displayValue: String,
            override val filterOptions: List<FilterListOptionUiModel>
        ) : Parcelable, FilterListCategoryUiModel()
    }

    @Parcelize
    data class FilterListOptionUiModel(
        val displayName: String,
        val isSelected: Boolean = false
    ) : Parcelable
}
