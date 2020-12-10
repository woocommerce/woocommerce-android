package com.woocommerce.android.ui.orders.tracking

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
internal abstract class AddOrderShipmentTrackingModule {
    companion object {
        @Provides
        fun provideDefaultArgs(fragment: AddOrderShipmentTrackingFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: AddOrderShipmentTrackingFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(AddOrderShipmentTrackingViewModel::class)
    abstract fun bindFactory(factory: AddOrderShipmentTrackingViewModel.Factory):
        ViewModelAssistedFactory<out ViewModel>
}
