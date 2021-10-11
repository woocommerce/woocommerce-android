package com.woocommerce.android.ui.main

interface MainNavigationRouter {
    fun isAtNavigationRoot(): Boolean
    fun isChildFragmentShowing(): Boolean

    fun showProductDetail(remoteProductId: Long, enableTrash: Boolean = false)
    fun showProductVariationDetail(remoteProductId: Long, remoteVariationId: Long)

    fun showOrderDetail(
        localSiteId: Int,
        localOrderId: Int = 0,
        remoteOrderId: Long,
        remoteNoteId: Long = 0,
        launchedFromNotification: Boolean = false
    )

    fun showOrderFilters()
    fun showAddProduct()
    fun showReviewDetail(
        remoteReviewId: Long,
        launchedFromNotification: Boolean,
        enableModeration: Boolean,
        tempStatus: String? = null
    )

    fun showProductFilters(
        stockStatus: String?,
        productType: String?,
        productStatus: String?,
        productCategory: String?,
        productCategoryName: String?
    )

    fun showFeedbackSurvey()
    fun showProductAddBottomSheet()
    fun showSettingsScreen()
}
