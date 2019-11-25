package com.woocommerce.android.ui.refunds

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
abstract class RefundItemsPickerModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: RefundItemsPickerDialog): Bundle? {
            return fragment.parentFragment?.arguments
        }

        @JvmStatic
        @Provides
        fun provideSavedStateRegistryOwner(fragment: RefundItemsPickerDialog): SavedStateRegistryOwner {
            return fragment.findNavController().getBackStackEntry(R.id.nav_graph_refunds)
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(IssueRefundViewModel::class)
    abstract fun bindFactory(factory: IssueRefundViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
