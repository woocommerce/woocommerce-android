package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.reviews.ProductReviewStatus.SPAM
import com.woocommerce.android.ui.reviews.ProductReviewStatus.TRASH
import com.woocommerce.android.viewmodel.combineWith
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter

interface ReviewModerationConsumer {
    val ReviewModerationConsumer.reviewModerationHandler: ReviewModerationHandler
    val ReviewModerationConsumer.rawReviewList: LiveData<List<ProductReview>>
    fun ReviewModerationConsumer.reloadReviewsFromCache()
}

val ReviewModerationConsumer.pendingReviewModerationStatus
    get() = reviewModerationHandler.pendingModerationStatus.asLiveData()

val ReviewModerationConsumer.reviewList
    get() = rawReviewList.combineWith(pendingReviewModerationStatus) { list, status ->
        if (status == null) return@combineWith list.orEmpty()
        list?.map {
            if (it.remoteId == status.review.remoteId) {
                it.copy(status = status.newStatus.toString())
            } else {
                it
            }
        }?.filter {
            it.status != TRASH.toString() &&
                it.status != SPAM.toString()
        }.orEmpty()
    }

suspend fun ReviewModerationConsumer.observeModerationEvents() {
    reviewModerationHandler.pendingModerationStatus
        .filter { it.actionStatus == ActionStatus.SUCCESS }
        .collect {
            reloadReviewsFromCache()
        }
}
