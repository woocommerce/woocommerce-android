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
abstract class ProductVariantModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: ProductVariantFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: ProductVariantFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(ProductVariantViewModel::class)
    abstract fun bindFactory(factory: ProductVariantViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
