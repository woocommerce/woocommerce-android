package com.woocommerce.android.ui.main

interface MainNavigationRouter {
    fun isAtNavigationRoot(): Boolean
    fun isChildFragmentShowing(): Boolean

    fun showProductDetail(remoteProductId: Long, enableTrash: Boolean = false)
    fun showOrderDetail(
        localSiteId: Int,
        localOrderId: Int = 0,
        remoteOrderId: Long,
        remoteNoteId: Long = 0,
        markComplete: Boolean = false
    )
    fun showAddProduct()
    fun showReviewDetail(remoteReviewId: Long, launchedFromNotification: Boolean, tempStatus: String? = null, enableModeration: Boolean = true)
    fun showProductFilters(stockStatus: String?, productType: String?, productStatus: String?)
    fun showFeedbackSurvey()
    fun showProductAddBottomSheet()
}
