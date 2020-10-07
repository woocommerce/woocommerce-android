package com.woocommerce.android.ui.orders.shippinglabels

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
abstract class PrintShippingLabelModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: PrintShippingLabelFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: PrintShippingLabelFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(PrintShippingLabelViewModel::class)
    abstract fun bindFactory(factory: PrintShippingLabelViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
