package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.R
import dagger.Module
import dagger.Provides

@Module
abstract class ProductCatalogVisibilityModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: ProductCatalogVisibilityFragment): Bundle? {
            return fragment.arguments
        }

        @JvmStatic
        @Provides
        fun provideSavedStateRegistryOwner(fragment: ProductCatalogVisibilityFragment): SavedStateRegistryOwner {
            return fragment.findNavController().getBackStackEntry(R.id.nav_graph_product_settings)
        }
    }
}
