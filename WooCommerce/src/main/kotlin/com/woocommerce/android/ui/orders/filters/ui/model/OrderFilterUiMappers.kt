package com.woocommerce.android.ui.orders.filters.ui.model

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.WCOrderStatusModel

fun WCOrderStatusModel.toFilterListOptionUiModel(resourceProvider: ResourceProvider) =
    OrderListFilterOptionUiModel(
        key = statusKey,
        displayName = getDisplayNameForOrderStatus(resourceProvider),
    )

private fun WCOrderStatusModel.getDisplayNameForOrderStatus(resourceProvider: ResourceProvider) =
    if (statusCount > 0) {
        resourceProvider.getString(R.string.orderfilters_order_status_with_count_filter_option, label, statusCount)
    } else {
        label
    }

fun OrderFilterDateRange.toFilterListOptionUiModel(resourceProvider: ResourceProvider) =
    OrderListFilterOptionUiModel(
        key = filterKey,
        displayName = resourceProvider.getString(stringResource),
    )
