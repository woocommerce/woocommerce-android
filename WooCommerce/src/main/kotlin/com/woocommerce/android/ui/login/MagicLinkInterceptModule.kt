package com.woocommerce.android.ui.login

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
abstract class MagicLinkInterceptModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(): Bundle? {
            return null
        }
    }
    @Binds
    @IntoMap
    @ViewModelKey(MagicLinkInterceptViewModel::class)
    abstract fun bindFactory(factory: MagicLinkInterceptViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: MagicLinkInterceptFragment): SavedStateRegistryOwner
}
