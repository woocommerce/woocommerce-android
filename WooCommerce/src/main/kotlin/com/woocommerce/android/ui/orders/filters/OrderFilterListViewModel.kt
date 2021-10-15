package com.woocommerce.android.ui.orders.filters

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListOptionUiModel.DateRangeFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListOptionUiModel.OrderStatusFilterOptionUiModel
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
    private val networkStatus: NetworkStatus,
) : ScopedViewModel(savedState) {

    private val _orderFilterCategories = MutableLiveData<List<FilterListCategoryUiModel>>()
    val orderFilterCategories: LiveData<List<FilterListCategoryUiModel>> = _orderFilterCategories

    private val _orderFilterOptions = MutableLiveData<List<FilterListOptionUiModel>>()
    val orderFilterOptions: LiveData<List<FilterListOptionUiModel>> = _orderFilterOptions

    private val _orderFilterCategoryViewState = MutableLiveData<OrderFilterCategoryListViewState>()
    val orderFilterCategoryViewState: LiveData<OrderFilterCategoryListViewState> = _orderFilterCategoryViewState

    private val _orderFilterOptionTitle = MutableLiveData<String>()
    val orderFilterOptionTitle: LiveData<String> = _orderFilterOptionTitle

    private var selectedFilterCategory: FilterListCategoryUiModel? = null

    init {
        _orderFilterCategories.value = buildFilterListUiModel()
    }

    private fun buildFilterListUiModel(): List<FilterListCategoryUiModel> {
        val currentOrderStatusFilterOptions = loadOrderStatusFilterOptions()
        return listOf(
            FilterListCategoryUiModel(
                displayName = resourceProvider.getString(R.string.orderfilters_order_status_filter),
                displayValue = getDisplayValueForOrderStatusSelectedFilters(currentOrderStatusFilterOptions),
                currentOrderStatusFilterOptions
            ),
            FilterListCategoryUiModel(
                displayName = resourceProvider.getString(R.string.orderfilters_date_range_filter),
                displayValue = getDisplayValueForOrderStatusSelectedFilters(currentOrderStatusFilterOptions),
                emptyList()
            )
        )
    }

    private fun getDisplayValueForOrderStatusSelectedFilters(orderStatusFilters: List<FilterListOptionUiModel>): String =
        if (isAnyOrderStatusSelected(orderStatusFilters)) {
            getNumberOfOrderStatusSelected(orderStatusFilters)
        } else {
            resourceProvider.getString(R.string.orderfilters_default_filter_value)
        }

    private fun getNumberOfOrderStatusSelected(orderStatusFilters: List<FilterListOptionUiModel>) =
        orderStatusFilters
            .drop(1)
            .filter { it.isSelected }
            .count()
            .toString()

    private fun isAnyOrderStatusSelected(orderStatusFilters: List<FilterListOptionUiModel>) =
        orderStatusFilters
            .drop(1)
            .any { it.isSelected }

    private fun loadOrderStatusFilterOptions(): List<FilterListOptionUiModel> {
        return listOf(
            OrderStatusFilterOptionUiModel("All", isSelected = true),
            OrderStatusFilterOptionUiModel("Cancelled"),
            OrderStatusFilterOptionUiModel("Completed"),
            OrderStatusFilterOptionUiModel("Failed"),
            OrderStatusFilterOptionUiModel("On hold"),
        )
    }

    fun onFilterCategoryClicked(filterCategory: FilterListCategoryUiModel) {
        selectedFilterCategory = filterCategory
        _orderFilterOptions.value = filterCategory.filterOptions
        _orderFilterOptionTitle.value = getOrderFilterOptionsTitle(filterCategory)
        triggerEvent(ShowOrderStatusFilterOptions)
    }

    private fun getOrderFilterOptionsTitle(filterCategory: FilterListCategoryUiModel): String {
        TODO("Not yet implemented")
    }

    fun onFilterOptionClicked(selectedFilterOption: FilterListOptionUiModel) {
        when (selectedFilterOption) {
            is OrderStatusFilterOptionUiModel -> updateOrderStatusSelectedFilters(selectedFilterOption)
            is DateRangeFilterOptionUiModel -> updateDateRangeSelectedFilters(selectedFilterOption)
        }
    }

    private fun updateDateRangeSelectedFilters(selectedFilterOption: FilterListOptionUiModel) {
        TODO("Not yet implemented")
    }

    private fun updateOrderStatusSelectedFilters(selectedOrderStatus: OrderStatusFilterOptionUiModel) {
        when {
            isAllOptionSelected(selectedOrderStatus) -> clearOrderStatusSelections()
            else -> unselectAllOption()
        }

        _orderFilterOptions.value?.let { optionsList ->
            val updatedOptionList = optionsList.map {
                when (it) {
                    selectedOrderStatus -> selectedOrderStatus.copy(isSelected = !selectedOrderStatus.isSelected)
                    else -> it
                }
            }
            _orderFilterOptions.value = updatedOptionList

            _orderFilterCategories.value?.let { filterCategories ->
                val updatedSelection = filterCategories.map { category ->
                    when (category.displayName) {
                        selectedFilterCategory?.displayName -> updateFilterSelectedCategory(category, updatedOptionList)
                        else -> category
                    }
                }
                _orderFilterCategories.value = updatedSelection
            }
        }
    }

    private fun unselectAllOption() {
        _orderFilterOptions.value = _orderFilterOptions.value
            ?.filterIsInstance<OrderStatusFilterOptionUiModel>()
            ?.mapIndexed { index, orderStatus ->
                if (index == 0) orderStatus.copy(isSelected = false)
                else orderStatus
            }
    }

    private fun isAllOptionSelected(selectedOrderStatusFilter: OrderStatusFilterOptionUiModel) =
        selectedOrderStatusFilter.displayName == resourceProvider.getString(R.string.orderfilters_default_filter_value)

    private fun clearOrderStatusSelections() {
        _orderFilterOptions.value = _orderFilterOptions.value
            ?.filterIsInstance<OrderStatusFilterOptionUiModel>()
            ?.map { it.copy(isSelected = false) }
    }

    private fun updateFilterSelectedCategory(
        filterCategory: FilterListCategoryUiModel,
        updatedOptionList: List<FilterListOptionUiModel>
    ) = filterCategory.copy(
        displayValue = getDisplayValueForOrderStatusSelectedFilters(updatedOptionList),
        filterOptions = updatedOptionList
    )

    fun onShowOrdersClicked() {
        TODO("Not yet implemented")
    }

    sealed class OrderFilterListEvent : MultiLiveEvent.Event() {
        object ShowOrderStatusFilterOptions : OrderFilterListEvent()
    }

    @Parcelize
    data class OrderFilterCategoryListViewState(
        val screenTitle: String,
        val displayClearButton: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class FilterListCategoryUiModel(
        val displayName: String,
        val displayValue: String,
        val filterOptions: List<FilterListOptionUiModel>
    ) : Parcelable

    sealed class FilterListOptionUiModel : Parcelable {
        abstract val displayName: String
        abstract val isSelected: Boolean

        @Parcelize
        data class OrderStatusFilterOptionUiModel(
            override val displayName: String,
            override val isSelected: Boolean = false
        ) : Parcelable, FilterListOptionUiModel()

        @Parcelize
        data class DateRangeFilterOptionUiModel(
            override val displayName: String,
            override val isSelected: Boolean = false
        ) : Parcelable, FilterListOptionUiModel()
    }
}
