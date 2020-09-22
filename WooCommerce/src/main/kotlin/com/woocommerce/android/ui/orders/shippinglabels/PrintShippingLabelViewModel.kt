package com.woocommerce.android.ui.orders.shippinglabels

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel

class PrintShippingLabelViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    private val repository: ShippingLabelRefundRepository,
    private val networkStatus: NetworkStatus,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<PrintShippingLabelViewModel>
}
