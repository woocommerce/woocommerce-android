package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class ShippingLabelRefundModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: ShippingLabelRefundFragment): Bundle? {
            return fragment.arguments
        }

        @JvmStatic
        @Provides
        fun provideSavedStateRegistryOwner(fragment: ShippingLabelRefundFragment): SavedStateRegistryOwner {
            return fragment.findNavController().getBackStackEntry(R.id.nav_graph_shipping_labels)
        }
    }
    @Binds
    @IntoMap
    @ViewModelKey(ShippingLabelRefundViewModel::class)
    abstract fun bindFactory(factory: ShippingLabelRefundViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
