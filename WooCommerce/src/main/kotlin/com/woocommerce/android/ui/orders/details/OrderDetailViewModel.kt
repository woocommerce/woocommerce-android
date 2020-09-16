package com.woocommerce.android.ui.orders.details

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel

@OpenClassOnDebug
class OrderDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<OrderDetailViewModel>
}
