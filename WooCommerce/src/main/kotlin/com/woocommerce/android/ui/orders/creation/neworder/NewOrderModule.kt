package com.woocommerce.android.ui.orders.creation.neworder

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
interface NewOrderModule {
    companion object {
        @Provides
        fun provideDefaultArgs(fragment: NewOrderFragment): Bundle? = fragment.arguments
    }

    @Binds
    fun bindSavedStateRegistryOwner(fragment: NewOrderFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(NewOrderViewModel::class)
    fun bindFactory(factory: NewOrderViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
