package com.woocommerce.android.ui.main

interface MainNavigationRouter {
    fun isAtNavigationRoot(): Boolean
    fun isChildFragmentShowing(): Boolean

    fun showOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNoteId: Long = 0, markComplete: Boolean = false)
    fun showProductDetail(remoteProductId: Long, enableTrash: Boolean = false)
    fun showAddProduct()
    fun showReviewDetail(remoteReviewId: Long, launchedFromNotification: Boolean, tempStatus: String? = null)
    fun showProductFilters(stockStatus: String?, productType: String?, productStatus: String?)
    fun showFeedbackSurvey()
    fun showProductAddBottomSheet()
}
