package com.woocommerce.android.ui.orders.filters

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
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

    init {
        _orderFilterCategories.value = buildFilterListUiModel()
    }

    private fun buildFilterListUiModel(): List<FilterListCategoryUiModel> {
        val currentOrderStatusFilterOptions = loadOrderStatusFilterOptions()
        return listOf(
            FilterListCategoryUiModel(
                displayName = resourceProvider.getString(R.string.orderfilters_order_status_filter),
                displayValue = getOrderStatusSelectedValue(currentOrderStatusFilterOptions),
                currentOrderStatusFilterOptions
            ),
            FilterListCategoryUiModel(
                displayName = resourceProvider.getString(R.string.orderfilters_date_range_filter),
                displayValue = getOrderStatusSelectedValue(currentOrderStatusFilterOptions),
                emptyList()
            )
        )
    }

    private fun getOrderStatusSelectedValue(currentOrderStatusFilterOptions: List<FilterListOptionUiModel>): String {
        return if (currentOrderStatusFilterOptions.any { it.isSelected }) {
            currentOrderStatusFilterOptions
                .filter { it.isSelected }
                .count()
                .toString()
        } else {
            resourceProvider.getString(R.string.orderfilters_default_filter_value)
        }
    }

    private fun loadOrderStatusFilterOptions(): List<FilterListOptionUiModel> {
        return listOf(
            FilterListOptionUiModel("All"),
            FilterListOptionUiModel("Cancelled"),
            FilterListOptionUiModel("Completed"),
            FilterListOptionUiModel("Failed"),
            FilterListOptionUiModel("On hold"),
        )
    }

    fun onFilterCategoryClicked(selectedFilterCategory: FilterListCategoryUiModel) {
        _orderFilterOptions.value = selectedFilterCategory.filterOptions
        triggerEvent(ShowOrderStatusFilterOptions)
    }

    fun onFilterOptionClicked(selectedFilterOption: FilterListOptionUiModel) {
        val updatedFilterOption = selectedFilterOption.copy(
            isSelected = !selectedFilterOption.isSelected
        )
        _orderFilterOptions.value?.let { optionsList ->
            val updatedOptionList = optionsList
                .map {
                    when (it) {
                        selectedFilterOption -> updatedFilterOption
                        else -> it
                    }
                }
            _orderFilterOptions.value = updatedOptionList
        }
    }

    fun onShowOrdersClicked() {
        TODO("Not yet implemented")
    }

    sealed class OrderFilterListEvent : MultiLiveEvent.Event() {
        object ShowOrderStatusFilterOptions : OrderFilterListEvent()
    }

    @Parcelize
    data class FilterListCategoryUiModel(
        val displayName: String,
        val displayValue: String,
        val filterOptions: List<FilterListOptionUiModel>
    ) : Parcelable

    @Parcelize
    data class FilterListOptionUiModel(
        val displayName: String,
        val isSelected: Boolean = false,
    ) : Parcelable
}
