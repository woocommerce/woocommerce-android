package com.woocommerce.android.ui.orders.filters.ui.model

import android.os.Parcelable
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderFilterCategory
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.parcelize.Parcelize

sealed class OrderFilterListEvent : MultiLiveEvent.Event() {
    object ShowOrderStatusFilterOptions : OrderFilterListEvent()
}

@Parcelize
data class OrderFilterCategoryListViewState(
    val screenTitle: String,
    val displayClearButton: Boolean = false
) : Parcelable

@Parcelize
data class FilterListCategoryUiModel(
    val categoryKey: OrderFilterCategory,
    val displayName: String,
    val displayValue: String,
    val orderFilterOptions: List<OrderListFilterOptionUiModel>
) : Parcelable

@Parcelize
data class OrderListFilterOptionUiModel(
    val key: String,
    val displayName: String,
    val isSelected: Boolean = false
) : Parcelable {
    companion object {
        const val DEFAULT_ALL_KEY = "All"
    }
}
