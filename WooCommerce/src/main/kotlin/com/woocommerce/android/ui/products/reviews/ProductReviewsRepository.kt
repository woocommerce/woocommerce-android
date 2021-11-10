package com.woocommerce.android.ui.products.reviews

import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import javax.inject.Inject

class ProductReviewsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore
) {
    companion object {
        private const val PRODUCT_REVIEW_STATUS_APPROVED = "approved"
        private const val PAGE_SIZE = WCProductStore.NUM_REVIEWS_PER_FETCH
    }

    private var offset = 0

    var canLoadMore: Boolean = false
        private set

    /**
     * Submits a fetch request to get a page of product reviews for the current site and [remoteProductId]
     */
    suspend fun fetchApprovedProductReviewsFromApi(
        remoteProductId: Long,
        loadMore: Boolean
    ): OnProductReviewChanged {
        val newOffset = if (loadMore) offset + PAGE_SIZE else 0

        val payload = FetchProductReviewsPayload(
            selectedSite.get(), offset,
            productIds = listOf(remoteProductId),
            filterByStatus = listOf(PRODUCT_REVIEW_STATUS_APPROVED)
        )
        val result = productStore.fetchProductReviews(payload)
        if (result.isError) {
            WooLog.e(
                REVIEWS,
                "Error fetching product review: " +
                    "${result.error?.type} - ${result.error?.message}"
            )
        } else {
            canLoadMore = result.canLoadMore
            offset = newOffset
        }
        return result
    }

    /**
     * Returns all product reviews for the current site and product from the local database
     */
    fun getProductReviewsFromDB(remoteProductId: Long): List<ProductReview> {
        return productStore.getProductReviewsForProductAndSiteId(
            localSiteId = selectedSite.get().id,
            remoteProductId = remoteProductId
        ).map { it.toAppModel() }
    }
}
