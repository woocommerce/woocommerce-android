package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.viewmodel.combineWith
import kotlinx.coroutines.flow.filter

interface ReviewModerationConsumer {
    val ReviewModerationConsumer.reviewModerationHandler: ReviewModerationHandler
    val ReviewModerationConsumer.rawReviewList: LiveData<List<ProductReview>>
    fun ReviewModerationConsumer.onReviewModerationSuccess()
}

val ReviewModerationConsumer.pendingReviewModerationStatus
    get() = reviewModerationHandler.pendingModerationStatus.asLiveData()

val ReviewModerationConsumer.reviewList
    get() = rawReviewList.combineWith(pendingReviewModerationStatus) { list, statuses ->
        if (statuses == null) return@combineWith list.orEmpty()
        list?.map { review ->
            val status = statuses.firstOrNull { it.review.remoteId == review.remoteId }
            if (status != null) {
                review.copy(status = status.newStatus.toString())
            } else {
                review
            }
        }?.filter {
            it.status != ProductReviewStatus.TRASH.toString() && it.status != ProductReviewStatus.SPAM.toString()
        }.orEmpty()
    }

suspend fun ReviewModerationConsumer.observeModerationEvents() {
    reviewModerationHandler.pendingModerationStatus
        .filter { statuses -> statuses.any { it.actionStatus == ActionStatus.SUCCESS } }
        .collect { onReviewModerationSuccess() }
}

fun ReviewModerationConsumer.undoModerationRequest(review: ProductReview) {
    reviewModerationHandler.undoOperation(review)
}
