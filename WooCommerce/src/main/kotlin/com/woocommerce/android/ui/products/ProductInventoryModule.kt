package com.woocommerce.android.ui.products

import android.os.Bundle
import androidx.savedstate.SavedStateRegistryOwner
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
abstract class ProductInventoryModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(): Bundle? {
            return null
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: ProductInventoryFragment): SavedStateRegistryOwner
}
