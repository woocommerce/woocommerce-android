package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.R
import dagger.Module
import dagger.Provides

@Module
abstract class ProductVisibilityModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: ProductVisibilityFragment): Bundle? {
            return fragment.arguments
        }

        @JvmStatic
        @Provides
        fun provideSavedStateRegistryOwner(fragment: ProductVisibilityFragment): SavedStateRegistryOwner {
            return fragment.findNavController().getBackStackEntry(R.id.nav_graph_product_settings)
        }
    }
}
