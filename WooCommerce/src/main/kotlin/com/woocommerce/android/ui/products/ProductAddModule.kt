package com.woocommerce.android.ui.products

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.reviews.ReviewDetailFragment
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class ProductAddModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: ProductAddFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(ProductAddViewModel::class)
    abstract fun bindFactory(factory: ProductAddViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: ProductAddFragment): SavedStateRegistryOwner
}
