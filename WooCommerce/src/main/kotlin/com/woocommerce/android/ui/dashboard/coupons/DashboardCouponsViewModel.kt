package com.woocommerce.android.ui.dashboard.coupons

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = DashboardCouponsViewModel.Factory::class)
class DashboardCouponsViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel
) : ScopedViewModel(savedStateHandle) {
    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardCouponsViewModel
    }
}
