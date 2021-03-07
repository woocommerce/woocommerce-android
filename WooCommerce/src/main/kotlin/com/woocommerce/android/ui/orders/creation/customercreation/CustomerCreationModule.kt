package com.woocommerce.android.ui.orders.creation.customercreation

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
interface CustomerCreationModule {
    companion object {
        @Provides
        fun provideDefaultArgs(fragment: CustomerCreationFragment): Bundle? = fragment.arguments
    }

    @Binds
    fun bindSavedStateRegistryOwner(fragment: CustomerCreationFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(CustomerCreationViewModel::class)
    fun bindFactory(factory: CustomerCreationViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
