package com.woocommerce.android.ui.orders.filters.model

import android.os.Parcelable
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.parcelize.Parcelize

sealed class OrderFilterListEvent : MultiLiveEvent.Event() {
    data class ShowOrderStatusFilterOptions(
        val category: OrderFilterCategoryUiModel
    ) : OrderFilterListEvent()
}

@Parcelize
data class OrderFilterCategoryListViewState(
    val screenTitle: String,
    val displayClearButton: Boolean = false
) : Parcelable

@Parcelize
data class OrderFilterCategoryUiModel(
    val categoryKey: OrderListFilterCategory,
    val displayName: String,
    val displayValue: String,
    val orderFilterOptions: List<OrderFilterOptionUiModel>
) : Parcelable

@Parcelize
data class OrderFilterOptionUiModel(
    val key: String,
    val displayName: String,
    val isSelected: Boolean = false
) : Parcelable {
    companion object {
        const val DEFAULT_ALL_KEY = "All"
    }
}
