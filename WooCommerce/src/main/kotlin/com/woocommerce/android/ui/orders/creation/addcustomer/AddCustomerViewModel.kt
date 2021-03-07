package com.woocommerce.android.ui.orders.creation.addcustomer

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel

class AddCustomerViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    repo: AddCustomerRepository
) : ScopedViewModel(savedState, dispatchers) {
    fun getItemCount(): Int {
    }

    fun getItemViewType(position: Int): Int {
    }

    fun bindView(customerItemView: CustomerItemView) {
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddCustomerViewModel>
}
