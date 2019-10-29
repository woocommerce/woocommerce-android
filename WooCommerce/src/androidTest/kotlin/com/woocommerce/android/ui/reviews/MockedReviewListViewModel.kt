package com.woocommerce.android.ui.reviews

import androidx.lifecycle.SavedStateHandle
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import org.wordpress.android.fluxc.Dispatcher

class MockedReviewListViewModel @AssistedInject constructor(
    dispatchers: CoroutineDispatchers,
    reviewRepository: ReviewListRepository,
    networkStatus: NetworkStatus,
    dispatcher: Dispatcher,
    selectedSite: SelectedSite,
    @Assisted arg0: SavedStateHandle
) : ReviewListViewModel(
        dispatchers,
        reviewRepository,
        networkStatus,
        dispatcher,
        selectedSite,
        arg0
) {
    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<MockedReviewListViewModel>
}
