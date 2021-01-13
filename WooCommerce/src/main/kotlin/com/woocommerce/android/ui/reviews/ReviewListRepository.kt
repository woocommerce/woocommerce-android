package com.woocommerce.android.ui.reviews

import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.ProductReviewProduct
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
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
import org.wordpress.android.fluxc.action.WCProductAction.UPDATE_PRODUCT_REVIEW_STATUS
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind.STORE_REVIEW
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
        private const val PAGE_SIZE = WCProductStore.NUM_REVIEWS_PER_FETCH
    }

    private var continuationReview: Continuation<Boolean>? = null
    private var continuationProduct: Continuation<Boolean>? = null
    private var continuationNotification: Continuation<Boolean>? = null
    private var continuationMarkAllRead: Continuation<RequestResult>? = null

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
     * Fetch product reviews and notifications from the API. Wait for both requests to complete. If the
     * fetch is already in progress return [RequestResult.NO_ACTION_NEEDED].
     *
     * @param [loadMore] if true, creates an offset to fetch the next page of [ProductReview]s
     * from the API.
     * @return the result of the fetch as a [RequestResult]
     */
    suspend fun fetchProductReviews(loadMore: Boolean): RequestResult {
        return if (!isFetchingProductReviews) {
            isLoadingMore = loadMore
            coroutineScope {
                val fetchNotifs = async {
                    /*
                     * Fetch notifications so we can match them to reviews to get the read state. This
                     * will wait for completion. If this fails we still consider fetching reviews to be successful since it
                     * failing won't block the user. Just log the exception.
                     */
                    fetchNotifications()
                }

                var wasFetchReviewsSuccess = false
                val fetchReviews = async {
                    wasFetchReviewsSuccess = fetchProductReviewsFromApi(loadMore)

                    /*
                     * Fetch any products associated with these reviews missing from the db.
                     */
                    if (wasFetchReviewsSuccess) {
                        getProductReviewsFromDB().map { it.remoteProductId }
                                .distinct()
                                .takeIf { it.isNotEmpty() }?.let { fetchProductsByRemoteId(it) }
                    }
                }

                // Wait for both to complete before continuing
                fetchNotifs.await()
                fetchReviews.await()

                if (wasFetchReviewsSuccess) SUCCESS else ERROR
            }
        } else NO_ACTION_NEEDED
    }

    /**
     * Fires the request to mark all product review notifications as read to the API. If there are
     * no unread product review notifications in the database, then the result will be
     * [RequestResult.NO_ACTION_NEEDED].
     *
     * @return the result of the action as a [RequestResult]
     */
    suspend fun markAllProductReviewsAsRead(): RequestResult {
        return if (getHasUnreadCachedProductReviews()) {
            val unreadProductReviews = notificationStore.getNotificationsForSite(
                    site = selectedSite.get(),
                    filterBySubtype = listOf(STORE_REVIEW.toString()))
            try {
                suspendCoroutineWithTimeout<RequestResult>(AppConstants.REQUEST_TIMEOUT) {
                    continuationMarkAllRead = it

                    val payload = MarkNotificationsReadPayload(unreadProductReviews)
                    dispatcher.dispatch(
                            NotificationActionBuilder.newMarkNotificationsReadAction(payload))

                    AnalyticsTracker.track(Stat.REVIEWS_MARK_ALL_READ)
                } ?: ERROR // block timed out. Return error.
            } catch (e: CancellationException) {
                WooLog.e(REVIEWS, "Exception encountered while fetching product reviews", e)
                ERROR
            }
        } else {
            WooLog.d(REVIEWS, "Mark all as read: No unread product reviews found. Exiting...")
            NO_ACTION_NEEDED
        }
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
        var cachedReviews = getProductReviewsFromDB().map { it.toAppModel() }
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
                    it.read = readValueByRemoteIdMap[it.remoteId] // if not found will stay null
                }
            }
        }
        return cachedReviews
    }

    /**
     * Checks the database for any product review notifications where [NotificationModel.#read] = false
     *
     * @return true if unread product reviews exist in db, else false
     */
    suspend fun getHasUnreadCachedProductReviews(): Boolean {
        return coroutineScope {
            notificationStore.hasUnreadNotificationsForSite(
                    site = selectedSite.get(),
                    filterBySubtype = listOf(STORE_REVIEW.toString()))
        }
    }

    /**
     * Fetch products from the API and suspends until finished.
     */
    private suspend fun fetchProductsByRemoteId(remoteProductIds: List<Long>) {
        try {
            suspendCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationProduct = it

                val payload = FetchProductsPayload(
                        selectedSite.get(),
                        remoteProductIds = remoteProductIds)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(REVIEWS, "Exception encountered while attempting to fetch products by remote ID", e)
        }
    }

    /**
     * Fetches notifications from the API. We use these results to populate [ProductReview.read].
     */
    private suspend fun fetchNotifications(): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationNotification = it

                val payload = FetchNotificationsPayload()
                dispatcher.dispatch(NotificationActionBuilder.newFetchNotificationsAction(payload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(REVIEWS, "Exception encountered while fetching product review notifications", e)
            false
        }
    }

    private suspend fun fetchProductReviewsFromApi(loadMore: Boolean): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                offset = if (loadMore) offset + PAGE_SIZE else 0
                isFetchingProductReviews = true
                continuationReview = it

                val payload = WCProductStore.FetchProductReviewsPayload(selectedSite.get(), offset)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductReviewsAction(payload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(REVIEWS, "Exception encountered while fetching product reviews", e)
            false
        }
    }

    /**
     * Returns a list of all [WCProductReviewModel]s for the active site.
     */
    private suspend fun getProductReviewsFromDB(): List<WCProductReviewModel> {
        return withContext(Dispatchers.IO) {
            productStore.getProductReviewsForSite(selectedSite.get())
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
                    filterBySubtype = listOf(STORE_REVIEW.toString())
            ).associate { it.getCommentId() to it.read }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_PRODUCTS) {
            if (event.isError) {
                AnalyticsTracker.track(Stat.REVIEWS_PRODUCTS_LOAD_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))

                WooLog.e(REVIEWS, "Error fetching matching product for product review: " +
                        "${event.error?.type} - ${event.error?.message}")
                continuationProduct?.resume(false)
            } else {
                AnalyticsTracker.track(Stat.REVIEWS_PRODUCTS_LOADED)
                continuationProduct?.resume(true)
            }
            continuationProduct = null
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductReviewChanged(event: OnProductReviewChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_REVIEWS) {
            isFetchingProductReviews = false
            if (event.isError) {
                AnalyticsTracker.track(Stat.REVIEWS_LOAD_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))

                WooLog.e(REVIEWS, "Error fetching product review: " +
                        "${event.error?.type} - ${event.error?.message}")
                continuationReview?.resume(false)
            } else {
                AnalyticsTracker.track(Stat.REVIEWS_LOADED, mapOf(
                        AnalyticsTracker.KEY_IS_LOADING_MORE to isLoadingMore))
                isLoadingMore = false
                canLoadMore = event.canLoadMore
                continuationReview?.resume(true)
            }
            continuationReview = null
        } else if (event.causeOfChange == UPDATE_PRODUCT_REVIEW_STATUS) {
            if (event.isError) {
                AnalyticsTracker.track(
                        Stat.REVIEW_ACTION_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))

                WooLog.e(REVIEWS, "${ReviewListFragment.TAG} - Error pushing product review status " +
                        "changes to server!: ${event.error?.type} - ${event.error?.message}")
            } else {
                AnalyticsTracker.track(Stat.REVIEW_ACTION_SUCCESS)
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        if (event.causeOfChange == FETCH_NOTIFICATIONS) {
            if (event.isError) {
                AnalyticsTracker.track(Stat.NOTIFICATIONS_LOAD_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))

                WooLog.e(REVIEWS, "Error fetching product review notifications: " +
                        "${event.error?.type} - ${event.error?.message}")
                continuationNotification?.resume(false)
            } else {
                AnalyticsTracker.track(Stat.NOTIFICATIONS_LOADED)

                continuationNotification?.resume(true)
            }
            continuationNotification = null
        } else if (event.causeOfChange == MARK_NOTIFICATIONS_READ) {
            // Since this can be called from other places, only process this event if we were the
            // one who submitted the request.
            continuationMarkAllRead?.let {
                if (event.isError) {
                    AnalyticsTracker.track(Stat.REVIEWS_MARK_ALL_READ_FAILED, mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                            AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))

                    WooLog.e(REVIEWS, "Error marking all reviews as read: " +
                            "${event.error?.type} - ${event.error?.message}")
                    it.resume(ERROR)
                } else {
                    AnalyticsTracker.track(Stat.REVIEWS_MARK_ALL_READ_SUCCESS)
                    it.resume(SUCCESS)
                }
                continuationMarkAllRead = null
            }
        }
    }
}
