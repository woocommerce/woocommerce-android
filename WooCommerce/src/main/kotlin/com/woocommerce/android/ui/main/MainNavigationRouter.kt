package com.woocommerce.android.ui.main

import com.woocommerce.android.model.ProductReview

interface MainNavigationRouter {
    fun isAtNavigationRoot(): Boolean
    fun isChildFragmentShowing(): Boolean

    fun showOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNoteId: Long = 0, markComplete: Boolean = false)
    fun showProductDetail(remoteProductId: Long)
    fun showReviewDetail(review: ProductReview, tempStatus: String? = null)
}
