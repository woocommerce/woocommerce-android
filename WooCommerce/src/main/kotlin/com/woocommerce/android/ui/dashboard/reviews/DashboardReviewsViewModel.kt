package com.woocommerce.android.ui.dashboard.reviews

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.reviews.ProductReviewStatus
import com.woocommerce.android.ui.reviews.ReviewListRepository
import com.woocommerce.android.ui.reviews.ReviewModerationHandler
import com.woocommerce.android.ui.reviews.ReviewModerationStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DashboardReviewsViewModel.Factory::class)
class DashboardReviewsViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val reviewListRepository: ReviewListRepository,
    private val reviewModerationHandler: ReviewModerationHandler,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        val supportedFilters = listOf(
            ProductReviewStatus.ALL,
            ProductReviewStatus.APPROVED,
            ProductReviewStatus.HOLD
        )

        @VisibleForTesting
        const val MAX_REVIEWS = 3
    }

    private val _refreshTrigger = MutableSharedFlow<DashboardViewModel.RefreshEvent>(extraBufferCapacity = 1)
    private val refreshTrigger = merge(parentViewModel.refreshTrigger, _refreshTrigger)
        .onStart { emit(DashboardViewModel.RefreshEvent()) }
    private val status = savedStateHandle.getStateFlow(viewModelScope, ProductReviewStatus.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = status
        .flatMapLatest {
            refreshTrigger.map { refresh -> Pair(refresh, it) }
        }
        .transformLatest { (refresh, status) ->
            emit(ViewState.Loading(status))
            emitAll(
                observeMostRecentReviews(forceRefresh = refresh.isForced, status = status)
                    .map { result ->
                        result.fold(
                            onSuccess = { reviews ->
                                ViewState.Success(reviews, status)
                            },
                            onFailure = { ViewState.Error }
                        )
                    }
            )
        }
        .asLiveData()

    fun onFilterSelected(status: ProductReviewStatus) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.REVIEWS.trackingIdentifier)
        this.status.value = status
    }

    fun onViewAllClicked() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.REVIEWS.trackingIdentifier)
        triggerEvent(OpenReviewsList)
    }

    fun onReviewClicked(review: ProductReview) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.REVIEWS.trackingIdentifier)
        triggerEvent(OpenReviewDetail(review))
    }

    fun onRetryClicked() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.REVIEWS.trackingIdentifier
            )
        )
        _refreshTrigger.tryEmit(DashboardViewModel.RefreshEvent())
    }

    private fun observeMostRecentReviews(
        forceRefresh: Boolean,
        status: ProductReviewStatus
    ) = channelFlow<Result<List<ProductReview>>> {
        val fetchBeforeEmit = forceRefresh || observeCachedReviews(status).first().isEmpty()

        if (fetchBeforeEmit) {
            reviewListRepository.fetchMostRecentReviews(status)
                .onFailure {
                    send(Result.failure(it))
                    return@channelFlow
                }
        }

        coroutineScope {
            val cacheJob = launch {
                observeCachedReviews(status)
                    .collect { cachedReviews ->
                        send(Result.success(cachedReviews))
                    }
            }

            if (!fetchBeforeEmit) {
                reviewListRepository.fetchMostRecentReviews(status)
                    .onFailure {
                        cacheJob.cancel()
                        send(Result.failure(it))
                    }
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun observeCachedReviews(status: ProductReviewStatus) =
        reviewModerationHandler.pendingModerationStatus.map { moderationStatus ->
            val cachedReviews = reviewListRepository.getCachedProductReviews()
                .filter { status == ProductReviewStatus.ALL || it.status == status.toString() }
                // We need just 3 review, but we will take an additional review to account for
                // any pending moderation requests
                .take(MAX_REVIEWS + 1)

            cachedReviews.applyModerationStatus(moderationStatus)
                .take(MAX_REVIEWS)
        }

    private fun List<ProductReview>.applyModerationStatus(
        moderationStatus: List<ReviewModerationStatus>
    ): List<ProductReview> {
        return map { review ->
            val status = moderationStatus.firstOrNull { it.review.remoteId == review.remoteId }
            if (status != null) {
                review.copy(status = status.newStatus.toString())
            } else {
                review
            }
        }.filter {
            it.status != ProductReviewStatus.TRASH.toString() && it.status != ProductReviewStatus.SPAM.toString()
        }
    }

    sealed interface ViewState {
        data class Loading(val selectedFilter: ProductReviewStatus) : ViewState
        data class Success(
            val reviews: List<ProductReview>,
            val selectedFilter: ProductReviewStatus
        ) : ViewState

        data object Error : ViewState
    }

    data object OpenReviewsList : MultiLiveEvent.Event()
    data class OpenReviewDetail(val review: ProductReview) : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardReviewsViewModel
    }
}
