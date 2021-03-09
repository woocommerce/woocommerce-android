package com.woocommerce.android.ui.orders.creation.addcustomer

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
interface AddCustomerModule {
    companion object {
        @Provides
        fun provideDefaultArgs(fragment: AddCustomerFragment): Bundle? = fragment.arguments

        @Provides
        fun provideLayoutInflater(fragment: AddCustomerFragment): LayoutInflater = fragment.layoutInflater
    }

    @Binds
    fun bindSavedStateRegistryOwner(fragment: AddCustomerFragment): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(AddCustomerViewModel::class)
    fun bindFactory(factory: AddCustomerViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
