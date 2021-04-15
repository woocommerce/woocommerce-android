package com.woocommerce.android.ui.orders

interface OrderProductActionListener {
    fun openOrderProductDetail(remoteProductId: Long)
    fun openOrderProductVariationDetail(remoteProductId: Long, remoteVariationId: Long)
}
