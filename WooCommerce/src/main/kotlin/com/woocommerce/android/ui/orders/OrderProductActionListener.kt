package com.woocommerce.android.ui.orders

interface OrderProductActionListener {
    fun openOrderProductDetail(remoteProductId: Long, remoteVariationId: Long = 0L)
}
