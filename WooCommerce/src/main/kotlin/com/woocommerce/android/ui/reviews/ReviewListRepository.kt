package com.woocommerce.android.ui.reviews

import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.ProductReviewProduct
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ReviewListRepository.RequestResult.ERROR
import com.woocommerce.android.ui.reviews.ReviewListRepository.RequestResult.SUCCESS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction.FETCH_NOTIFICATIONS
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATIONS_READ
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_REVIEWS
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsPayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
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
    private val notificationStore: NotificationStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val PAGE_SIZE = WCProductStore.NUM_REVIEWS_PER_FETCH
    }

    private var continuationReview: Continuation<Boolean>? = null
    private var continuationProduct: Continuation<Boolean>? = null
    private var continuationNotification: Continuation<Boolean>? = null
    private var continuationMarkAllRead: Continuation<RequestResult>? = null

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
     * Fetch product reviews from the API, wait for it to complete, and then query the db
     * for the fetched reviews.
     *
     * @param [loadMore] if true, creates an offset to fetch the next page of [ProductReview]s
     * from the API.
     * @return List of [ProductReview]
     */
    suspend fun fetchAndLoadProductReviews(loadMore: Boolean = false): List<ProductReview> {
        if (!isFetchingProductReviews) {
            coroutineScope {
                /*
                 * Fetch notifications so we can match them to reviews to get the read state. This
                 * will wait for completion.
                 */
                val fetchNotifs = async { fetchNotifications() }

                val fetchReviews = async {
                    suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                        offset = if (loadMore) offset + PAGE_SIZE else 0
                        isFetchingProductReviews = true
                        continuationReview = it

                        val payload = WCProductStore.FetchProductReviewsPayload(
                                selectedSite.get(),
                                offset
                        )
                        dispatcher.dispatch(
                                WCProductActionBuilder.newFetchProductReviewsAction(
                                        payload
                                )
                        )
                    }

                    /*
                     * Fetch any products associated with these reviews missing from the db
                     */
                    getProductReviews().map { it.remoteProductId }.distinct().takeIf { it.isNotEmpty() }?.let {
                        fetchProductsByRemoteId(it)
                    }
                }

                // Wait for both to complete before continuing
                fetchNotifs.await()
                fetchReviews.await()
            }
        }

        return getCachedProductReviews()
    }

    /**
     * Create a distinct list of products associated with the reviews already in the db, then
     * pass that list to get a map of those products from the db. Only reviews that have an existing
     * cached product will be returned.
     *
     * Also populates the [ProductReview.read] field with the value of a matching Notification, or if
     * one doesn't exist, it is set to true.
     */
    suspend fun getCachedProductReviews(): List<ProductReview> {
        var cachedReviews = getProductReviews()
        val readValueByRemoteIdMap = getReviewNotifReadValueByRemoteIdMap()

        if (cachedReviews.isNotEmpty()) {
            val relatedProducts = cachedReviews.map { it.remoteProductId }.distinct()
            val productsMap = getProductsByRemoteIdMap(relatedProducts)
            cachedReviews = cachedReviews.filter {
                // Only returns reviews that have a matching product in the db.
                productsMap.containsKey(it.remoteProductId) && productsMap[it.remoteProductId] != null
            }.also { review ->
                review.forEach {
                    it.product = productsMap[it.remoteProductId]
                    it.read = readValueByRemoteIdMap[it.remoteId] ?: true
                }
            }
        }
        return cachedReviews
    }

    suspend fun getHasUnreadReviews(): Boolean {
        return withContext(Dispatchers.IO) {
            notificationStore.hasUnreadNotificationsForSite(
                    site = selectedSite.get(),
                    filterBySubtype = listOf(Subkind.STORE_REVIEW.toString()))
        }
    }

    /**
     * Mark all product review notifications as read.
     */
    suspend fun markAllReviewsAsRead(): RequestResult {
        return if (getHasUnreadReviews()) {
            val unreadProductReviews = notificationStore.getNotificationsForSite(
                    site = selectedSite.get(),
                    filterBySubtype = listOf(Subkind.STORE_REVIEW.toString()))

            suspendCoroutineWithTimeout<RequestResult>(ACTION_TIMEOUT) {
                continuationMarkAllRead = it

                val payload = MarkNotificationsReadPayload(unreadProductReviews)
                dispatcher.dispatch(NotificationActionBuilder.newMarkNotificationsReadAction(payload))
            }!!
        } else {
            WooLog.d(REVIEWS, "Mark all as read: No unread product reviews found. Exiting...")
            RequestResult.NO_ACTION_NEEDED
        }
    }

    /**
     * Fetch products from the API and suspends until finished.
     */
    private suspend fun fetchProductsByRemoteId(remoteProductIds: List<Long>) {
        suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
            continuationProduct = it

            val payload = FetchProductsPayload(selectedSite.get(), remoteProductIds = remoteProductIds)
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
        }
    }

    /**
     * Fetches notifications from the API. We use these results to populate [ProductReview.read].
     */
    private suspend fun fetchNotifications() {
        suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
            continuationNotification = it

            val payload = FetchNotificationsPayload()
            dispatcher.dispatch(NotificationActionBuilder.newFetchNotificationsAction(payload))
        }
    }

    /**
     * Returns a list of all [ProductReview]s for the active site.
     */
    private suspend fun getProductReviews(): List<ProductReview> {
        return withContext(Dispatchers.IO) {
            productStore.getProductReviewsForSite(selectedSite.get()).map { it.toAppModel() }
        }
    }

    /**
     * Queries the db for a [org.wordpress.android.fluxc.model.WCProductModel] matching the
     * provided [remoteProductId] and returns it as a [ProductReviewProduct] or null if not found.
     */
    private suspend fun getProductByRemoteId(remoteProductId: Long): ProductReviewProduct? {
        return withContext(Dispatchers.IO) {
            productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)?.let {
                ProductReviewProduct(it.remoteProductId, it.name, it.externalUrl)
            }
        }
    }

    /**
     * Returns a map of [ProductReviewProduct] by the remote_product_id pulled from the db.
     */
    private suspend fun getProductsByRemoteIdMap(remoteProductIds: List<Long>): Map<Long, ProductReviewProduct?> {
        return withContext(Dispatchers.IO) {
            remoteProductIds.associateWith { getProductByRemoteId(it) }
        }
    }

    /**
     * Uses the product review notifications to create a map of
     * [org.wordpress.android.fluxc.model.notification.NotificationModel.read] by [ProductReview.remoteId].
     */
    private suspend fun getReviewNotifReadValueByRemoteIdMap(): Map<Long, Boolean> {
        return withContext(Dispatchers.IO) {
            notificationStore.getNotificationsForSite(
                    site = selectedSite.get(),
                    filterBySubtype = listOf(Subkind.STORE_REVIEW.toString())
            ).associate { it.getCommentId() to it.read }
        }
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        if (event.causeOfChange == FETCH_NOTIFICATIONS) {
            if (event.isError) {
                // TODO AMANDA : track fetch notifications failed
                WooLog.e(REVIEWS, "Error fetching product review notifications: ${event.error.message}")
                continuationNotification?.resume(false)
            } else {
                // TODO AMANDA : track fetch notifications success
                continuationNotification?.resume(true)
            }
        } else if (event.causeOfChange == MARK_NOTIFICATIONS_READ) {
            if (event.isError) {
                // TODO AMANDA : track mark notifications read error
                WooLog.e(REVIEWS, "Error marking all reviews as read: ${event.error.message}")
                continuationMarkAllRead?.resume(ERROR)
            } else {
                // TODO AMANDA : track mark notifications read success
                continuationMarkAllRead?.resume(SUCCESS)
            }
        }
    }

    enum class RequestResult {
        SUCCESS,
        ERROR,
        NO_ACTION_NEEDED
    }
}
