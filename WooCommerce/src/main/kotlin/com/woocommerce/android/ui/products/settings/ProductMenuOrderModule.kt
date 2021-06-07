package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.R
import dagger.Module
import dagger.Provides

@Module
abstract class ProductMenuOrderModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: ProductMenuOrderFragment): Bundle? {
            return fragment.arguments
        }

        @JvmStatic
        @Provides
        fun provideSavedStateRegistryOwner(fragment: ProductMenuOrderFragment): SavedStateRegistryOwner {
            return fragment.findNavController().getBackStackEntry(R.id.nav_graph_product_settings)
        }
    }
}
