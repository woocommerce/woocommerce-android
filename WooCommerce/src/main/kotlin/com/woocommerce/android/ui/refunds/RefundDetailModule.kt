package com.woocommerce.android.ui.refunds

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import dagger.Module
import dagger.Binds
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class RefundDetailModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: RefundDetailFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(RefundDetailViewModel::class)
    abstract fun bindFactory(factory: RefundDetailViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: RefundDetailFragment): SavedStateRegistryOwner
}
