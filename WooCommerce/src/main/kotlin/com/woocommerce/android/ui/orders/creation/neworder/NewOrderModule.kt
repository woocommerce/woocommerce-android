package com.woocommerce.android.ui.orders.creation.neworder

import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.R.id
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.orders.creation.common.OrderCreationViewModel
import com.woocommerce.android.ui.orders.creation.common.OrderCreationViewModel.Factory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class NewOrderModule {
    companion object {
        @Provides
        fun provideDefaultArgs(fragment: NewOrderFragment) = fragment.arguments

        @Provides
        fun provideSavedStateRegistryOwner(fragment: NewOrderFragment): SavedStateRegistryOwner {
            return fragment.findNavController().getBackStackEntry(id.nav_graph_order_creation)
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(OrderCreationViewModel::class)
    abstract fun bindFactory(factory: Factory): ViewModelAssistedFactory<out ViewModel>
}
