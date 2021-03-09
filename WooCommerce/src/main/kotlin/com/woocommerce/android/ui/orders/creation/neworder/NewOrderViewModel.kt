package com.woocommerce.android.ui.orders.creation.neworder

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.orders.creation.common.navigation.OrderCreationNavigationTarget
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel

class NewOrderViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    fun onAddNewCustomerButtonClicked() {
        triggerEvent(OrderCreationNavigationTarget.AddCustomer)
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<NewOrderViewModel>
}
