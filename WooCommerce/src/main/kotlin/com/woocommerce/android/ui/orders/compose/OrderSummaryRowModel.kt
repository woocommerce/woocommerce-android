package com.woocommerce.android.ui.orders.compose

import androidx.annotation.ColorRes

data class OrderSummaryRowModel(
    val number: String,
    val date: String,
    val customerName: String,
    val status: String,
    @ColorRes val statusColor: Int,
    val totalPrice: String,
    val isPosOrder: Boolean = false,
)
