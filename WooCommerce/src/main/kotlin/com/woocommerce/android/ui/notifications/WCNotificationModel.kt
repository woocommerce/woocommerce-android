package com.woocommerce.android.ui.notifications

import org.wordpress.android.fluxc.model.order.OrderIdentifier

sealed class WCNotificationModel(val id: Int, val title: String, val desc: String, val dateCreated: String) {
    class Order(
        id: Int,
        title: String,
        desc: String,
        dateCreated: String,
        val orderIdentifier: OrderIdentifier,
        val remoteOrderId: Long
    ) : WCNotificationModel(id, title, desc, dateCreated)

    class Review(
        id: Int,
        title: String,
        desc: String,
        dateCreated: String,
        val productId: Int,
        val rating: Float,
        val productUrl: String
    ) : WCNotificationModel(id, title, desc, dateCreated)
}
