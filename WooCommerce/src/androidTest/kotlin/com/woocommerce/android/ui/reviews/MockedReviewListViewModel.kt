package com.woocommerce.android.ui.reviews

import com.woocommerce.android.viewmodel.SavedState
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
        arg0,
        dispatchers,
        networkStatus,
        dispatcher,
        selectedSite,
        reviewRepository
) {
    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<MockedReviewListViewModel>
}
