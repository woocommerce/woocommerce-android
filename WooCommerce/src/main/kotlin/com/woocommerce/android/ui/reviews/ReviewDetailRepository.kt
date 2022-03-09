package com.woocommerce.android.ui.reviews

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.model.*
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind.STORE_REVIEW
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class ReviewDetailRepository @Inject constructor(
    private val productStore: WCProductStore,
    private val notificationStore: NotificationStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val TAG = "ReviewDetailRepository"
    }

    suspend fun fetchProductReview(remoteReviewId: Long): RequestResult {
        if (fetchProductReviewFromApi(remoteReviewId)) {
            getProductReviewFromDb(remoteReviewId)?.let {
                if (fetchProductByRemoteId(it.remoteProductId, remoteReviewId)) {
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

    suspend fun markNotificationAsRead(notification: NotificationModel, remoteReviewId: Long) {
        if (!notification.read) {
            notification.read = true
            trackMarkNotificationAsReadStarted(notification, remoteReviewId)
            val result = notificationStore.markNotificationsRead(MarkNotificationsReadPayload(listOf(notification)))
            trackMarkNotificationReadResult(result)
        }
    }

    private fun trackMarkNotificationAsReadStarted(notification: NotificationModel, remoteReviewId: Long) {
        AnalyticsTracker.track(
            AnalyticsEvent.REVIEW_MARK_READ,
            mapOf(
                AnalyticsTracker.KEY_ID to remoteReviewId,
                AnalyticsTracker.KEY_NOTE_ID to notification.remoteNoteId
            )
        )
    }

    private fun trackMarkNotificationReadResult(result: OnNotificationChanged) {
        if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.REVIEW_MARK_READ_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to result.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to result.error?.message
                )
            )

            WooLog.e(
                REVIEWS,
                "$TAG - Error marking review notification as read: " +
                    "${result.error?.type} - ${result.error?.message}"
            )
        } else {
            AnalyticsTracker.track(AnalyticsEvent.REVIEW_MARK_READ_SUCCESS)
        }
    }

    private suspend fun fetchProductByRemoteId(remoteProductId: Long, remoteReviewId: Long): Boolean {
        val payload = WCProductStore.FetchSingleProductPayload(selectedSite.get(), remoteProductId)
        val result = productStore.fetchSingleProduct(payload)

        return if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.REVIEW_PRODUCT_LOAD_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to result.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to result.error?.message
                )
            )

            WooLog.e(
                REVIEWS,
                "Error fetching matching product for product review: " +
                    "${result.error?.type} - ${result.error?.message}"
            )
            false
        } else {
            AnalyticsTracker.track(
                AnalyticsEvent.REVIEW_PRODUCT_LOADED,
                mapOf(
                    AnalyticsTracker.KEY_ID to remoteProductId,
                    AnalyticsTracker.KEY_REVIEW_ID to remoteReviewId
                )
            )
            true
        }
    }

    private suspend fun fetchProductReviewFromApi(remoteReviewId: Long): Boolean {
        val payload = WCProductStore.FetchSingleProductReviewPayload(selectedSite.get(), remoteReviewId)
        val result = productStore.fetchSingleProductReview(payload)
        if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.REVIEW_LOAD_FAILED,
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
            AnalyticsTracker.track(AnalyticsEvent.REVIEW_LOADED, mapOf(AnalyticsTracker.KEY_ID to remoteReviewId))
        }
        return !result.isError
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
}
