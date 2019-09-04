package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.base.BaseRepository

abstract class ProductReviewsRepositoryContract : BaseRepository {
    var canLoadMore: Boolean = false
        protected set

    abstract suspend fun fetchProductReviews(loadMore: Boolean): RequestResult
    abstract suspend fun markAllProductReviewsAsRead(): RequestResult

    abstract suspend fun getCachedProductReviews(): List<ProductReview>
    abstract suspend fun getCachedProductReviewById(remoteId: Long): ProductReview?
    abstract suspend fun getHasUnreadCachedProductReviews(): Boolean
}
