package com.woocommerce.android.ui.reviews

import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toProductReviewProductModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.RequestResult.ERROR
import com.woocommerce.android.ui.reviews.RequestResult.SUCCESS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATIONS_READ
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT_REVIEW
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind.STORE_REVIEW
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductReviewChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ReviewDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val notificationStore: NotificationStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val TAG = "ReviewDetailRepository"
    }

    private var localNoteId: Int = 0

    private var continuationReview: Continuation<Boolean>? = null
    private var continuationProduct: Continuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchProductReview(remoteId: Long): RequestResult {
        if (fetchProductReviewFromApi(remoteId)) {
            getProductReviewFromDb(remoteId)?.let {
                if (fetchProductByRemoteId(it.remoteProductId)) {
                    return SUCCESS
                }
            }
        }
        return ERROR
    }

    suspend fun getCachedProductReview(remoteId: Long): ProductReview? {
        return getProductReviewFromDb(remoteId)?.toAppModel()?.let { review ->
            getProductFromDb(review.remoteProductId)?.toProductReviewProductModel()?.let { product ->
                review.also { it.product = product }
            }
        }
    }

    suspend fun getCachedNotificationForReview(remoteReviewId: Long): NotificationModel? {
        return withContext(Dispatchers.IO) {
            notificationStore.getNotificationsForSite(
                    site = selectedSite.get(),
                    filterBySubtype = listOf(STORE_REVIEW.toString())
            ).firstOrNull { it.getCommentId() == remoteReviewId }
        }
    }

    suspend fun markNotificationAsRead(notification: NotificationModel) {
        if (!notification.read) {
            try {
                localNoteId = notification.noteId

                suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                    notification.read = true
                    val payload = MarkNotificationsReadPayload(listOf(notification))
                    dispatcher.dispatch(NotificationActionBuilder.newMarkNotificationsReadAction(payload))
                }
            } catch (e: CancellationException) {
                WooLog.e(REVIEWS, "Exception encountered while marking notification as read", e)
            }
        }
    }

    private suspend fun fetchProductByRemoteId(remoteProductId: Long): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationProduct = it

                val payload = WCProductStore.FetchSingleProductPayload(selectedSite.get(), remoteProductId)
                dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
            } ?: false // Timed out
        } catch (e: CancellationException) {
            WooLog.e(REVIEWS, "Exception encountered while fetching a single product", e)
            false
        }
    }

    private suspend fun fetchProductReviewFromApi(remoteId: Long): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationReview = it

                val payload = WCProductStore.FetchSingleProductReviewPayload(selectedSite.get(), remoteId)
                dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductReviewAction(payload))
            } ?: false // Timed out
        } catch (e: CancellationException) {
            WooLog.e(REVIEWS, "Exception encountered while fetching a single product review", e)
            false
        }
    }

    private suspend fun getProductReviewFromDb(remoteId: Long): WCProductReviewModel? {
        return withContext(Dispatchers.IO) {
            productStore.getProductReviewByRemoteId(selectedSite.get().id, remoteId)
        }
    }

    private suspend fun getProductFromDb(remoteProductId: Long): WCProductModel? {
        return withContext(Dispatchers.IO) {
            productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductReviewChanged(event: OnProductReviewChanged) {
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT_REVIEW) {
            if (event.isError) {
                // TODO AMANDA : track fetch single product review failed
                WooLog.e(REVIEWS, "Error fetching product review: ${event.error.message}")
                continuationReview?.resume(false)
            } else {
                // TODO AMANDA : track fetch single product review success
                continuationReview?.resume(true)
            }
            continuationReview = null
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT) {
            if (event.isError) {
                // TODO AMANDA : track fetch single product failed
                WooLog.e(REVIEWS, "Error fetching matching product for product review: ${event.error.message}")
                continuationProduct?.resume(false)
            } else {
                // TODO AMANDA : track fetch single product success
                continuationProduct?.resume(true)
            }
            continuationProduct = null
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        if (event.causeOfChange == MARK_NOTIFICATIONS_READ) {
            if (event.changedNotificationLocalIds.contains(localNoteId)) {
                if (event.isError) {
                    WooLog.e(REVIEWS, "$TAG - Error marking review notification as read!")
                    localNoteId = 0
                }
            }
        }
    }
}
