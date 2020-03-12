package com.woocommerce.android.ui.main

interface MainNavigationRouter {
    fun isAtNavigationRoot(): Boolean
    fun isChildFragmentShowing(): Boolean
    fun getActiveToolbarTitle(): String

    fun showOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNoteId: Long = 0, markComplete: Boolean = false)
    fun showProductDetail(remoteProductId: Long)
    fun showReviewDetail(remoteReviewId: Long, launchedFromNotification: Boolean, tempStatus: String? = null)
}
