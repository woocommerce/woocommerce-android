package com.woocommerce.android.ui.main

interface MainNavigationRouter {
    fun showOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNoteId: Long = 0, markComplete: Boolean = false)
    fun showProductDetail(remoteProductId: Long)
}
