package com.woocommerce.android.ui.dashboard.reviews

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.reviews.ProductReviewStatus
import com.woocommerce.android.ui.reviews.ReviewListRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

@HiltViewModel(assistedFactory = DashboardReviewsViewModel.Factory::class)
class DashboardReviewsViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val reviewListRepository: ReviewListRepository
) : ScopedViewModel(savedStateHandle) {
    private val refreshTrigger = parentViewModel.refreshTrigger
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
        this.status.value = status
    }

    private fun observeMostRecentReviews(
        forceRefresh: Boolean,
        status: ProductReviewStatus
    ) = flow<Result<List<ProductReview>>> {
        if (forceRefresh) {
            reviewListRepository.fetchMostRecentReviews(status)
                .onFailure {
                    emit(Result.failure(it))
                    return@flow
                }
        }

        emit(Result.success(getCachedReviews(status)))

        if (!forceRefresh) {
            reviewListRepository.fetchMostRecentReviews(status)
                .onFailure {
                    emit(Result.failure(it))
                }
            emit(Result.success(getCachedReviews(status)))
        }
    }

    @Suppress("MagicNumber")
    private suspend fun getCachedReviews(status: ProductReviewStatus) =
        reviewListRepository.getCachedProductReviews()
            .filter { status == ProductReviewStatus.ALL || it.status == status.toString() }
            .take(3)

    sealed interface ViewState {
        data class Loading(val selectedFilter: ProductReviewStatus) : ViewState
        data class Success(
            val reviews: List<ProductReview>,
            val selectedFilter: ProductReviewStatus
        ) : ViewState

        data object Error : ViewState
    }

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardReviewsViewModel
    }
}
