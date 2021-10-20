package com.woocommerce.android.ui.orders.filters.ui.model

import android.os.Parcelable
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
    val key: String,
    val displayName: String,
    val isSelected: Boolean = false
) : Parcelable
