package com.woocommerce.android.ui.base

interface TopLevelFragmentRouter {
    fun showOrderList(orderStatusFilter: String? = null)
    fun showNotificationDetail(remoteNoteId: Long)
}
