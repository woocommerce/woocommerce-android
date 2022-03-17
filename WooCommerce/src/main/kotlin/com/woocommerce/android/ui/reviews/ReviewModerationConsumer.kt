package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.viewmodel.combineWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

interface ReviewModerationConsumer {
    val ReviewModerationConsumer.reviewModerationHandler: ReviewModerationHandler
    val ReviewModerationConsumer.coroutineScope: CoroutineScope

    val pendingReviewModerationStatus: LiveData<ReviewModerationStatus>
        get() = reviewModerationHandler.pendingModerationStatus.asLiveData()

    val ReviewModerationConsumer.mutableReviewList: MutableLiveData<List<ProductReview>>

    fun ReviewModerationConsumer.reloadReviewsFromCache()

    companion object {
        val ReviewModerationConsumer.reviewList: LiveData<List<ProductReview>>
            get() = mutableReviewList.combineWithModerationStatus(pendingReviewModerationStatus)

        fun ReviewModerationConsumer.observeModerationEvents() {
            coroutineScope.launch {
                reviewModerationHandler.pendingModerationStatus
                    .filter { it.actionStatus == ActionStatus.SUCCESS }
                    .collect { reloadReviewsFromCache() }
            }
        }

        private fun LiveData<List<ProductReview>>.combineWithModerationStatus(
            statusLiveData: LiveData<ReviewModerationStatus>
        ): LiveData<List<ProductReview>> {
            return combineWith(statusLiveData) { list, status ->
                if (status == null) return@combineWith list.orEmpty()
                list?.map {
                    if (it.remoteId == status.review.remoteId) {
                        it.copy(status = status.newStatus.toString())
                    } else {
                        it
                    }
                }?.filter {
                    it.status != ProductReviewStatus.TRASH.toString() && it.status != ProductReviewStatus.SPAM.toString()
                }.orEmpty()
            }
        }
    }
}
