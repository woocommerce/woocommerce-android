package com.woocommerce.android.push

import com.woocommerce.android.AppPrefsWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewsNotificationsHandler @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper
) {

    private val unseenReviewsCount: MutableStateFlow<Int> = MutableStateFlow(getCurrentUnreadCount())

    private fun getCurrentUnreadCount(): Int {
        //Refresh in case reviews have been marked as read in other device
//        viewModelScope.launch
//            // First we set the cached value
//            _moreMenuViewState.value = _moreMenuViewState.value?.copy(
//                moreMenuItems = generateMenuButtons(getCachedUnseenReviewsCount())
//            )
//
//            // Then we fetch from API the refreshed value and update the UI again
//            when (reviewListRepository.fetchProductReviews(loadMore = false)) {
//                RequestResult.SUCCESS,
//                RequestResult.NO_ACTION_NEEDED -> {
//
//                    _moreMenuViewState.value = _moreMenuViewState.value?.copy(
//                        moreMenuItems = generateMenuButtons(getCachedUnseenReviewsCount())
//                    )
//                }
//                else -> {
//                }
//            }
//        }
        return 0
    }

//    private suspend fun getCachedUnseenReviewsCount() =
//        reviewListRepository.getCachedProductReviews()
//            .filter { it.read == false }
//            .count()

    fun observeUnseenCount(): Flow<Int> = unseenReviewsCount

    fun updateUnseenCountBy(newValue: Int) {
        unseenReviewsCount.update { oldValue ->
            oldValue + newValue
        }
    }

    fun clearUnseenCount() {
        unseenReviewsCount.update { 0 }
    }
}
