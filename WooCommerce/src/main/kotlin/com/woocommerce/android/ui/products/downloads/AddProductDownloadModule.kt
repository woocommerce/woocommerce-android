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
abstract class AddProductDownloadModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: AddProductDownloadBottomSheetFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: AddProductDownloadBottomSheetFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(AddProductDownloadViewModel::class)
    abstract fun bindFactory(factory: AddProductDownloadViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
