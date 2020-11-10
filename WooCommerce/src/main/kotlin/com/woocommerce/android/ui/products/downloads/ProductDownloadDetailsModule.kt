package com.woocommerce.android.ui.products.downloads

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
abstract class ProductDownloadDetailsModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: ProductDownloadDetailsFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: ProductDownloadDetailsFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(ProductDownloadDetailsViewModel::class)
    abstract fun bindFactory(factory: ProductDownloadDetailsViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
