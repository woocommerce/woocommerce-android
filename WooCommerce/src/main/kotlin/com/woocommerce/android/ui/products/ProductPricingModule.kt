package com.woocommerce.android.ui.products

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.products.variations.VariationDetailFragment
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class ProductPricingModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: ProductPricingFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: ProductPricingFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(ProductPricingViewModel::class)
    abstract fun bindFactory(factory: ProductPricingViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
