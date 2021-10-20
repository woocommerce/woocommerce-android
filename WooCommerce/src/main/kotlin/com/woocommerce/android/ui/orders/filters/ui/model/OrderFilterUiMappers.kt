package com.woocommerce.android.ui.orders.filters.ui.model

import org.wordpress.android.fluxc.model.WCOrderStatusModel

fun WCOrderStatusModel.toFilterListOptionUiModel() =
    OrderListFilterOptionUiModel(
        key = statusKey,
        displayName = label,
    )
