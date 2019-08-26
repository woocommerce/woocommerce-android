package com.woocommerce.android.ui.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

typealias ReviewsList = List<ProductReview>

class ReviewListViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val selectedSite: SelectedSite,
    private val reviewRepository: ReviewRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(mainDispatcher) {
    private val reviews = MutableLiveData<List<ProductReview>>()

    private val _reviewsData = MediatorLiveData<ReviewsList>()
    val reviewsData: LiveData<ReviewsList> = _reviewsData

    private val _isSkeletonShown = MutableLiveData<Boolean>()
    val isSkeletonShown: LiveData<Boolean> = _isSkeletonShown

    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    init {
        // TODO AMANDA
    }

    fun start() {
        // TODO AMANDA
    }

    override fun onCleared() {
        super.onCleared()

        reviewRepository.onCleanup()
    }

    private fun loadReviews() {
        launch {
            val reviewsInDb = reviewRepository.getProductReviews()
            if (reviewsInDb.isNotEmpty()) {
                // TODO display reviews we have

                // TODO fetch any updated reviews

                // Show the "updating" progress
            } else {
                // TODO fetch reviews

                _isSkeletonShown.value = true
            }
            _isSkeletonShown.value = false
        }
    }

    private suspend fun fetchReviews() {
        if (networkStatus.isConnected()) {
            val fetchedReviews = reviewRepository.fetchProductReviews()
            if (fetchedReviews.isNotEmpty()) {
                reviews.value = fetchedReviews
            } else {
                // TODO show the empty view
            }
        } else {
            // Network is not connected
            _showSnackbarMessage.value = R.string.offline_error
            _isSkeletonShown.value = false
        }
    }
}
