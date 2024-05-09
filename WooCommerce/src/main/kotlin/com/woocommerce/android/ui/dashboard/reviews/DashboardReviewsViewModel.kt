package com.woocommerce.android.ui.dashboard.reviews

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = DashboardReviewsViewModel.Factory::class)
class DashboardReviewsViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel
) : ScopedViewModel(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardReviewsViewModel
    }
}
