package com.woocommerce.android.ui.reviews

import com.woocommerce.android.AppConstants
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.ProductReviewProduct
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.RequestResult.*
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind.STORE_REVIEW
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.*
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import javax.inject.Inject

class ReviewListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val notificationStore: NotificationStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val PAGE_SIZE = WCProductStore.NUM_REVIEWS_PER_FETCH
    }

    private var continuationProduct = ContinuationWrapper<Boolean>(REVIEWS)

    private var offset = 0
    private var unreadReviewsOffset = 0
    private var isFetchingProductReviews = false
    private var unreadProductReviewIds: List<Long> = emptyList()

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
     * @return the result of the fetch as a [FetchReviewsResult]
     */
    suspend fun fetchProductReviews(
        loadMore: Boolean,
        remoteProductId: Long? = null
    ): Flow<FetchReviewsResult> =
        channelFlow {
            if (!isFetchingProductReviews) {
                coroutineScope {
                    launch {
                        val fetchNotificationsResult = fetchNotifications()
                        send(
                            FetchReviewsResult.NotificationsFetched(
                                if (fetchNotificationsResult.isSuccess) SUCCESS else ERROR
                            )
                        )
                    }

                    launch {
                        val wasFetchReviewsSuccess = fetchProductReviewsFromApi(loadMore, remoteProductId)
                        /*
                         * Fetch any products associated with these reviews missing from the db.
                         */
                        if (wasFetchReviewsSuccess) {
                            getProductReviewsFromDB().map { it.remoteProductId }
                                .distinct()
                                .takeIf { it.isNotEmpty() }?.let { fetchProductsByRemoteId(it) }
                        }
                        send(FetchReviewsResult.ReviewsFetched(if (wasFetchReviewsSuccess) SUCCESS else ERROR))
                    }
                }
            } else {
                send(FetchReviewsResult.NothingFetched)
            }
        }

    /**
     * Fetch the most recent product reviews from the API and notifications. this fetches only the first page
     * from the API, and doesn't delete previously cached reviews.
     *
     * @param [status] the status of the reviews to fetch
     * @return the result of the fetch as a [Result]
     */
    suspend fun fetchMostRecentReviews(
        status: ProductReviewStatus
    ): Result<Unit> = coroutineScope {
        val reviewsTask = async {
            val payload = WCProductStore.FetchProductReviewsPayload(
                site = selectedSite.get(),
                filterByStatus = status.takeIf { it != ProductReviewStatus.ALL }?.let { listOf(it.toString()) }
            )

            productStore.fetchProductReviews(
                payload = payload,
                deletePreviouslyCachedReviews = false
            ).let { result ->
                if (result.isError) {
                    Result.failure(OnChangedException(result.error))
                } else {
                    Result.success(Unit)
                }
            }
        }

        val notificationsTask = async {
            fetchNotifications()
        }

        reviewsTask.await()
            .onFailure { return@coroutineScope Result.failure(it) }
        notificationsTask.await()
            .onFailure { return@coroutineScope Result.failure(it) }

        Result.success(Unit)
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
                filterBySubtype = listOf(STORE_REVIEW.toString())
            )
            trackMarkAllProductReviewsAsReadStarted()
            val result = notificationStore.markNotificationsRead(MarkNotificationsReadPayload(unreadProductReviews))
            trackMarkAllProductReviewsAsReadResult(result)
            if (result.isError) ERROR else SUCCESS
        } else {
            WooLog.d(REVIEWS, "Mark all as read: No unread product reviews found. Exiting...")
            NO_ACTION_NEEDED
        }
    }

    private fun trackMarkAllProductReviewsAsReadStarted() {
        AnalyticsTracker.track(AnalyticsEvent.REVIEWS_MARK_ALL_READ)
    }

    private fun trackMarkAllProductReviewsAsReadResult(result: OnNotificationChanged) {
        if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.REVIEWS_MARK_ALL_READ_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to result.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to result.error?.message
                )
            )

            WooLog.e(
                REVIEWS,
                "Error marking all reviews as read: " +
                    "${result.error?.type} - ${result.error?.message}"
            )
        } else {
            AnalyticsTracker.track(AnalyticsEvent.REVIEWS_MARK_ALL_READ_SUCCESS)
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
    suspend fun getCachedProductReviews(productId: Long? = null): List<ProductReview> {
        var cachedReviews = getProductReviewsFromDB(productId).map { it.toAppModel() }
        val readValueByRemoteIdMap = getReviewNotifReadValueByRemoteIdMap()

        if (cachedReviews.isNotEmpty()) {
            val relatedProducts = cachedReviews.map { it.remoteProductId }.distinct()
            val productsMap = getProductsByRemoteIdMap(relatedProducts)
            cachedReviews = cachedReviews
                .filter {
                    // Only returns reviews that have a matching product in the db.
                    productsMap.containsKey(it.remoteProductId) && productsMap[it.remoteProductId] != null
                }.onEach {
                    it.product = productsMap[it.remoteProductId]
                    it.read = readValueByRemoteIdMap[it.remoteId] // if not found will stay null
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
                filterBySubtype = listOf(STORE_REVIEW.toString())
            )
        }
    }

    /**
     * Uses unread product review notification [NotificationModel.commentId] to then fetch the
     * specific unread product reviews from the API by using the #commentId as #reviewId in the
     * request payload.
     * If [productId] is provided, then only unread notifications for that product will be fetched.
     */
    suspend fun fetchOnlyUnreadProductReviews(loadMore: Boolean, productId: Long? = null): RequestResult {
        unreadProductReviewIds = notificationStore.getNotificationsForSite(
            site = selectedSite.get(),
            filterBySubtype = listOf(STORE_REVIEW.toString())
        )
            .filter { !it.read && if (productId == null) true else it.meta?.ids?.post == productId }
            .map { it.getCommentId() }
            .sortedByDescending { it }

        if (loadMore) unreadReviewsOffset += PAGE_SIZE else unreadReviewsOffset = 0
        val unreadProductReviewIdsToFetch = unreadProductReviewIds
            .drop(unreadReviewsOffset)
            .take(PAGE_SIZE)

        if (unreadProductReviewIdsToFetch.isNotEmpty()) {
            val result = productStore.fetchProductReviews(
                WCProductStore.FetchProductReviewsPayload(
                    site = selectedSite.get(),
                    reviewIds = unreadProductReviewIdsToFetch,
                    offset = 0 // Must be zero so the API filters only by ids and not page offset
                ),
                deletePreviouslyCachedReviews = false
            )
            return if (result.isError) ERROR else SUCCESS
        }
        return NO_ACTION_NEEDED
    }

    /**
     * Returns a list of [ProductReview]s from the db matching the [unreadProductReviewIds] list.
     * If the unread review ids list is empty, then an empty list is returned.
     */
    suspend fun getCachedUnreadProductReviews(): List<ProductReview> =
        withContext(Dispatchers.IO) {
            if (unreadProductReviewIds.isNotEmpty()) {
                productStore.getProductReviewsByReviewId(unreadProductReviewIds)
                    .map { it.toAppModel() }
                    .map { it.copy(read = false) }
                    .sortedByDescending { it.remoteId }
            } else {
                emptyList()
            }
        }

    /**
     * Fetch products from the API and suspends until finished.
     */
    private suspend fun fetchProductsByRemoteId(remoteProductIds: List<Long>) {
        continuationProduct.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = FetchProductsPayload(
                selectedSite.get(),
                remoteProductIds = remoteProductIds
            )
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
        }
    }

    /**
     * Fetches notifications from the API. We use these results to populate [ProductReview.read].
     */
    private suspend fun fetchNotifications(): Result<Unit> {
        val payload = FetchNotificationsPayload()
        val event = dispatcher.dispatchAndAwait<FetchNotificationsPayload, OnNotificationChanged>(
            NotificationActionBuilder.newFetchNotificationsAction(payload)
        )

        return when {
            event.isError -> {
                AnalyticsTracker.track(
                    AnalyticsEvent.NOTIFICATIONS_LOAD_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message
                    )
                )

                WooLog.e(
                    REVIEWS,
                    "Error fetching product review notifications: " +
                        "${event.error?.type} - ${event.error?.message}"
                )
                Result.failure(OnChangedException(event.error))
            }

            else -> {
                AnalyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_LOADED)
                Result.success(Unit)
            }
        }
    }

    private suspend fun fetchProductReviewsFromApi(loadMore: Boolean, remoteProductId: Long?): Boolean {
        val newOffset = if (loadMore) offset + PAGE_SIZE else 0
        isFetchingProductReviews = true
        val payload = WCProductStore.FetchProductReviewsPayload(
            site = selectedSite.get(),
            offset = newOffset,
            productIds = if (remoteProductId != null) listOf(remoteProductId) else emptyList()
        )

        val result = productStore.fetchProductReviews(
            payload = payload,
            deletePreviouslyCachedReviews = !loadMore
        )
        isFetchingProductReviews = false
        if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.REVIEWS_LOAD_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to result.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to result.error?.message
                )
            )

            WooLog.e(
                REVIEWS,
                "Error fetching product review: " +
                    "${result.error?.type} - ${result.error?.message}"
            )
        } else {
            AnalyticsTracker.track(
                AnalyticsEvent.REVIEWS_LOADED,
                mapOf(
                    AnalyticsTracker.KEY_IS_LOADING_MORE to loadMore
                )
            )
            canLoadMore = result.canLoadMore
            offset = newOffset
        }
        return !result.isError
    }

    /**
     * Returns a list of all [WCProductReviewModel]s for the active site.
     */
    private suspend fun getProductReviewsFromDB(productId: Long? = null): List<WCProductReviewModel> {
        return withContext(Dispatchers.IO) {
            productId?.let { productId ->
                productStore.getProductReviewsForProductAndSiteId(selectedSite.get().id, productId)
            } ?: productStore.getProductReviewsForSite(selectedSite.get())
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
                AnalyticsTracker.track(
                    AnalyticsEvent.REVIEWS_PRODUCTS_LOAD_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message
                    )
                )

                WooLog.e(
                    REVIEWS,
                    "Error fetching matching product for product review: " +
                        "${event.error?.type} - ${event.error?.message}"
                )
                continuationProduct.continueWith(false)
            } else {
                AnalyticsTracker.track(AnalyticsEvent.REVIEWS_PRODUCTS_LOADED)
                continuationProduct.continueWith(true)
            }
        }
    }

    sealed class FetchReviewsResult {
        data class ReviewsFetched(val requestResult: RequestResult) : FetchReviewsResult()
        data class NotificationsFetched(val requestResult: RequestResult) : FetchReviewsResult()
        data object NothingFetched : FetchReviewsResult()
    }
}
