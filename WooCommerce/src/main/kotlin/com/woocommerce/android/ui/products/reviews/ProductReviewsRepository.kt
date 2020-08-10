package com.woocommerce.android.ui.products.reviews

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_REVIEWS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ProductReviewsRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore
) {
    companion object {
        private const val PRODUCT_REVIEW_STATUS_APPROVED = "approved"
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val PAGE_SIZE = WCProductStore.NUM_REVIEWS_PER_FETCH
    }

    private var continuationReviews: Continuation<Boolean>? = null

    private var offset = 0
    private var isFetchingProductReviews = false
    private var isLoadingMore = false

    var canLoadMore: Boolean = false
        private set

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Submits a fetch request to get a page of product reviews for the current site and [remoteProductId]
     */
    suspend fun fetchApprovedProductReviewsFromApi(
        remoteProductId: Long,
        loadMore: Boolean
    ): List<ProductReview> {
        try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                offset = if (loadMore) offset + PAGE_SIZE else 0
                isFetchingProductReviews = true
                continuationReviews = it

                val payload = FetchProductReviewsPayload(
                    selectedSite.get(), offset,
                    productIds = listOf(remoteProductId),
                    filterByStatus = listOf(PRODUCT_REVIEW_STATUS_APPROVED)
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductReviewsAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "Exception encountered while fetching reviews for product $remoteProductId", e)
        }

        return getProductReviewsFromDB(remoteProductId)
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductReviewChanged(event: OnProductReviewChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_REVIEWS) {
            isFetchingProductReviews = false
            if (event.isError) {
                AnalyticsTracker.track(
                    Stat.REVIEWS_LOAD_FAILED, mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))

                WooLog.e(
                    REVIEWS, "Error fetching product review: " +
                    "${event.error?.type} - ${event.error?.message}")
                continuationReviews?.resume(false)
            } else {
                AnalyticsTracker.track(
                    Stat.REVIEWS_LOADED, mapOf(
                    AnalyticsTracker.KEY_IS_LOADING_MORE to isLoadingMore))
                isLoadingMore = false
                canLoadMore = event.canLoadMore
                continuationReviews?.resume(true)
            }
            continuationReviews = null
        }
    }
}
