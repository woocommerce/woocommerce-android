package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class ShippingCarrierRatesModule {
    companion object {
        @Provides
        fun provideDefaultArgs(fragment: ShippingCarrierRatesFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: ShippingCarrierRatesFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(ShippingCarrierRatesViewModel::class)
    abstract fun bindFactory(factory: ShippingCarrierRatesViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
