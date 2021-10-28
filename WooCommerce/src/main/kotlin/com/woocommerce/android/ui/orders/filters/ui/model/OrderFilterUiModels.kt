package com.woocommerce.android.ui.orders.filters.ui.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderListFilterCategory
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
data class OrderFilterCategoryUiModel(
    val categoryKey: OrderListFilterCategory,
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

enum class OrderFilterDateRange(@StringRes val stringResource: Int, val filterKey: String) {
    TODAY(R.string.orderfilters_date_range_filter_today, "Today"),
    LAST_2_DAYS(R.string.orderfilters_date_range_filter_last_two_days, "Last2Days"),
    THIS_WEEK(R.string.orderfilters_date_range_filter_this_week, "ThisWeek"),
    THIS_MONTH(R.string.orderfilters_date_range_filter_this_month, "ThisMonth");

    companion object {
        private val valueMap = values().associateBy(OrderFilterDateRange::filterKey)
        fun fromValue(filterKey: String) = valueMap[filterKey]
    }
}
