package com.woocommerce.android.ui.products

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
abstract class VariationDetailModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: VariationDetailFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: VariationDetailFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(VariationDetailViewModel::class)
    abstract fun bindFactory(factory: VariationDetailViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
