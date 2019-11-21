package com.woocommerce.android.ui.reviews

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
internal abstract class ReviewDetailModule {
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
    @ViewModelKey(ReviewDetailViewModel::class)
    internal abstract fun bindFactory(factory: ReviewDetailViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: ReviewDetailFragment): SavedStateRegistryOwner
}
