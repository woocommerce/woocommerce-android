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
internal abstract class AddOrderTrackingProviderListModule {
    companion object {
        @Provides
        fun provideDefaultArgs(fragment: AddOrderTrackingProviderListFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: AddOrderTrackingProviderListFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(AddOrderTrackingProviderListViewModel::class)
    abstract fun bindFactory(factory: AddOrderTrackingProviderListViewModel.Factory):
        ViewModelAssistedFactory<out ViewModel>
}
