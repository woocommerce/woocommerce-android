package com.woocommerce.android.ui.main

import org.wordpress.android.fluxc.model.notification.NotificationModel

interface MainNavigationRouter {
    fun isAtNavigationRoot(): Boolean
    fun isChildFragmentShowing(): Boolean

    fun showOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNoteId: Long = 0, markComplete: Boolean = false)
    fun showProductDetail(remoteProductId: Long)
    fun showReviewDetail(notification: NotificationModel, tempStatus: String? = null)
}
