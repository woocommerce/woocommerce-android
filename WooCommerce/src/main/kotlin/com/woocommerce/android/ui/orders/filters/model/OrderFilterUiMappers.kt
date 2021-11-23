package com.woocommerce.android.ui.orders.filters.model

import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.filters.data.DateRange
import com.woocommerce.android.ui.orders.filters.data.DateRange.CUSTOM_RANGE
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_2_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_30_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.LAST_7_DAYS
import com.woocommerce.android.ui.orders.filters.data.DateRange.TODAY
import com.woocommerce.android.ui.orders.filters.data.DateRangeFilterOption
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider

fun OrderStatusOption.toOrderFilterOptionUiModel(resourceProvider: ResourceProvider) =
    OrderFilterOptionUiModel(
        key = key,
        displayName = getDisplayNameForOrderStatusFilter(resourceProvider),
        isSelected = isSelected
    )

fun OrderStatusOption.getDisplayNameForOrderStatusFilter(resourceProvider: ResourceProvider) =
    if (statusCount > 0) {
        resourceProvider.getString(R.string.orderfilters_order_status_with_count_filter_option, label, statusCount)
    } else {
        label
    }

fun DateRangeFilterOption.toOrderFilterOptionUiModel(resourceProvider: ResourceProvider, dateUtils: DateUtils) =
    OrderFilterOptionUiModel(
        key = dateRange.filterKey,
        displayName = dateRange.toDisplayName(resourceProvider),
        displayValue = toDisplayDateRange(startDate, endDate, dateUtils),
        isSelected = isSelected
    )

fun toDisplayDateRange(startMillis: Long, endMillis: Long, dateUtils: DateUtils): String =
    when {
        startMillis > 0 && endMillis > 0 ->
            "${dateUtils.toDisplayMMMddYYYYDate(startMillis)} - ${dateUtils.toDisplayMMMddYYYYDate(endMillis)}"
        startMillis > 0 -> dateUtils.toDisplayMMMddYYYYDate(startMillis)
        endMillis > 0 -> dateUtils.toDisplayMMMddYYYYDate(endMillis)
        else -> ""
    } ?: ""

fun DateRange.toDisplayName(resourceProvider: ResourceProvider): String =
    resourceProvider.getString(
        when (this) {
            TODAY -> R.string.orderfilters_date_range_filter_today
            LAST_2_DAYS -> R.string.orderfilters_date_range_filter_last_two_days
            LAST_7_DAYS -> R.string.orderfilters_date_range_filter_last_7_days
            LAST_30_DAYS -> R.string.orderfilters_date_range_filter_last_30_days
            CUSTOM_RANGE -> R.string.orderfilters_date_range_filter_custom_range
        }
    )
