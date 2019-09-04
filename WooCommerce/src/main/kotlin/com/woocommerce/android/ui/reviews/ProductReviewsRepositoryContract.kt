package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview

interface ProductReviewsRepositoryContract {
    suspend fun fetchProductReviews(loadMore: Boolean): RequestResult
    suspend fun markAllProductReviewsAsRead(): RequestResult

    suspend fun getCachedProductReviews(): List<ProductReview>
    suspend fun getCachedProductReviewById(remoteId: Long): ProductReview?
    suspend fun getHasUnreadCachedProductReviews(): Boolean
}
