package com.woocommerce.android.ui.orders.details

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
abstract class OrderDetailModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: OrderDetailFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: OrderDetailFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(OrderDetailViewModel::class)
    abstract fun bindFactory(factory: OrderDetailViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
