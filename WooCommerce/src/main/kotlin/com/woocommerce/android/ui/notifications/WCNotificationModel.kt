package com.woocommerce.android.ui.notifications

sealed class WCNotificationModel(val id: Int, val title: String, val desc: String, val dateCreated: String) {
    class Order(
        id: Int,
        title: String,
        desc: String,
        dateCreated: String
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
