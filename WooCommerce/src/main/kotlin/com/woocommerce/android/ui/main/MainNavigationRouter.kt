package com.woocommerce.android.ui.main

import android.view.View

interface MainNavigationRouter {
    fun isAtNavigationRoot(): Boolean
    fun isChildFragmentShowing(): Boolean

    fun showProductDetail(remoteProductId: Long, enableTrash: Boolean = false)
    fun showProductDetailWithSharedTransition(
        remoteProductId: Long,
        sharedView: View,
        enableTrash: Boolean = false
    )
    fun showProductVariationDetail(remoteProductId: Long, remoteVariationId: Long)

    fun showOrderDetail(
        orderId: Long,
        remoteNoteId: Long = 0,
        launchedFromNotification: Boolean = false
    )

    fun showOrderDetailWithSharedTransition(
        orderId: Long,
        remoteNoteId: Long = 0,
        sharedView: View
    )

    fun showAddProduct()
    fun showReviewDetail(
        remoteReviewId: Long,
        launchedFromNotification: Boolean,
        enableModeration: Boolean,
        tempStatus: String? = null
    )
    fun showReviewDetailWithSharedTransition(
        remoteReviewId: Long,
        launchedFromNotification: Boolean,
        enableModeration: Boolean,
        sharedView: View,
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
    fun showSettingsScreen()
}
