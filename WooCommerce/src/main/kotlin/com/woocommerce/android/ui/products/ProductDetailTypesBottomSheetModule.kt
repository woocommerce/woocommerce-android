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
abstract class ProductDetailTypesBottomSheetModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: ProductDetailTypesBottomSheetFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(ProductDetailTypesBottomSheetViewModel::class)
    abstract
    fun bindFactory(factory: ProductDetailTypesBottomSheetViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract
    fun bindSavedStateRegistryOwner(fragment: ProductDetailTypesBottomSheetFragment): SavedStateRegistryOwner
}
