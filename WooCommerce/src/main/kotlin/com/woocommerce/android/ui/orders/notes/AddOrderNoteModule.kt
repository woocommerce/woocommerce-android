package com.woocommerce.android.ui.orders.notes

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
internal abstract class AddOrderNoteModule {

    companion object {
        @Provides
        fun provideDefaultArgs(fragment: AddOrderNoteFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(AddOrderNoteViewModel::class)
    abstract fun bindFactory(factory: AddOrderNoteViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: AddOrderNoteFragment): SavedStateRegistryOwner

}
