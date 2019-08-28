package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.ProductReviewProduct
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_REVIEWS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ReviewListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val PAGE_SIZE = WCProductStore.NUM_REVIEWS_PER_FETCH
    }

    private var continuationReview: Continuation<Boolean>? = null
    private var continuationProduct: Continuation<Boolean>? = null
    private var offset = 0
    private var isFetchingProductReviews = false
    var canLoadMoreReviews = false

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Fetch product reviews from the API, waits for it to complete, and then queries the db
     * for the fetched reviews.
     *
     * @param [loadMore] if true, creates an offset to fetch the next page of [ProductReview]s
     * from the API.
     * @return List of [ProductReview]
     */
    suspend fun fetchAndLoadProductReviews(loadMore: Boolean = false): List<ProductReview> {
        if (!isFetchingProductReviews) {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                offset = if (loadMore) offset + PAGE_SIZE else 0
                isFetchingProductReviews = true
                continuationReview = it

                val payload = WCProductStore.FetchProductReviewsPayload(selectedSite.get(), offset)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductReviewsAction(payload))
            }
        }

        return getProductReviews()
    }

    /**
     * Fetch products from the API, wait for it to complete, and then query the db
     * for the fetched reviews.
     *
     * @return List of [ProductReviewProduct]
     */
    private suspend fun fetchAndLoadProductsByRemoteId(remoteProductIds: List<Long>): List<ProductReviewProduct> {
        suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
            continuationProduct = it

            val payload = FetchProductsPayload(selectedSite.get(), remoteProductIds = remoteProductIds)
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
        }

        return productStore.getProductsByRemoteIds(selectedSite.get(), remoteProductIds)
                .map { ProductReviewProduct(it.remoteProductId, it.name, it.externalUrl) }
    }

    /**
     * Returns a list of all [ProductReview]s for the active site.
     */
    fun getProductReviews(): List<ProductReview> =
            productStore.getProductReviewsForSite(selectedSite.get()).map { it.toAppModel() }

    /**
     * Queries the db for a [org.wordpress.android.fluxc.model.WCProductModel] matching the
     * provided [remoteProductId] and returns it as a [ProductReviewProduct] or null if not found.
     */
    private fun getProductByRemoteId(remoteProductId: Long): ProductReviewProduct? {
        return productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)?.let {
             ProductReviewProduct(it.remoteProductId, it.name, it.externalUrl)
        }
    }

    /**
     * Takes a list of remote product_id's and returns a map of [ProductReviewProduct] by
     * the remote_product_id.
     */
    suspend fun getProductsByRemoteIdMap(remoteProductIds: List<Long>): Map<Long, ProductReviewProduct?> {
        /*
         * Create a list of remote product IDs not existing in the database and perform
         * a batch fetch from the API. Code will stop here until the fetch is complete.
         */
        remoteProductIds.filter { getProductByRemoteId(it) == null }.takeIf { it.isNotEmpty() }?.let {
            fetchAndLoadProductsByRemoteId(it)
        }

        return remoteProductIds.associateWith { getProductByRemoteId(it) }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_PRODUCTS) {
            if (event.isError) {
                // TODO AMANDA : track fetch products failed
                WooLog.e(REVIEWS, "Error fetching matching product for product review: ${event.error.message}")
                continuationProduct?.resume(false)
            } else {
                // TODO AMANDA : track fetch products success
                continuationProduct?.resume(true)
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductReviewChanged(event: OnProductReviewChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_REVIEWS) {
            isFetchingProductReviews = false
            if (event.isError) {
                // TODO AMANDA : track fetch product reviews failed
                WooLog.e(REVIEWS, "Error fetching product review: ${event.error.message}")
                continuationReview?.resume(false)
            } else {
                // TODO AMANDA : track fetch product reviews success
                canLoadMoreReviews = event.canLoadMore
                continuationReview?.resume(true)
            }
        }
    }
}
