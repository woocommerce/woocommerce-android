package com.woocommerce.android.ui.shortcuts


// Adding this to trigger file change check and lint in CI

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R

enum class AppShortcut(
    val id: String,
    val action: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    Payments(
        id = "payments_dynamic",
        action = "com.woocommerce.android.payments",
        label = R.string.more_menu_button_payments,
        icon = R.drawable.ic_more_menu_payments
    ),
    CreateOrder(
        id = "create_order_dynamic",
        action = "com.woocommerce.android.ordercreation",
        label = R.string.orderlist_create_order_button_description,
        icon = R.drawable.ic_menu_orders_list
    )
}
