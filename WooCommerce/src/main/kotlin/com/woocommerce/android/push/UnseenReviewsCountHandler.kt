package com.woocommerce.android.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.ui.reviews.ReviewListRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnseenReviewsCountHandler @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val reviewListRepository: ReviewListRepository,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {

    private val unseenReviewsCount: MutableStateFlow<Int> = MutableStateFlow(0)

    init {
        updateCurrentUnseenCount()
    }

    fun observeUnseenCount(): Flow<Int> = unseenReviewsCount

    fun updateUnseenCountBy(newValue: Int) {
        unseenReviewsCount.update { oldValue ->
            appPrefsWrapper.updateUnseenReviewCount(oldValue + newValue)
            oldValue + newValue
        }
    }

    fun clearUnseenCount() {
        appPrefsWrapper.updateUnseenReviewCount(0)
        unseenReviewsCount.update { 0 }
    }

    private fun updateCurrentUnseenCount() {
        unseenReviewsCount.update { appPrefsWrapper.getUnseenReviewCount() }

        //Refresh in case reviews have been marked as read in other device or from core
        appCoroutineScope.launch {
            when (reviewListRepository.fetchProductReviews(loadMore = false)) {
                RequestResult.SUCCESS,
                RequestResult.NO_ACTION_NEEDED -> {
                    val unseenCount = getCachedUnseenReviewsCount()
                    appPrefsWrapper.updateUnseenReviewCount(unseenCount)
                    unseenReviewsCount.update { unseenCount }
                }
                else -> {}
            }
        }
    }

    private suspend fun getCachedUnseenReviewsCount() =
        reviewListRepository.getCachedProductReviews()
            .filter { it.read == false }
            .count()
}
