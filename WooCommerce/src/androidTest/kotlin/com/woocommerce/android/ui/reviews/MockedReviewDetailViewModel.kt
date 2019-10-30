package com.woocommerce.android.ui.reviews

import androidx.lifecycle.SavedStateHandle
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers

class MockedReviewDetailViewModel @AssistedInject constructor(
    dispatchers: CoroutineDispatchers,
    reviewRepository: ReviewDetailRepository,
    networkStatus: NetworkStatus,
    @Assisted arg0: SavedStateHandle
) : ReviewDetailViewModel(
        arg0,
        dispatchers,
        networkStatus,
        reviewRepository
) {
    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<MockedReviewDetailViewModel>
}
