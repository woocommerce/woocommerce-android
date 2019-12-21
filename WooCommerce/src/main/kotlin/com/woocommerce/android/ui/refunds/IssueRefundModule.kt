package com.woocommerce.android.ui.refunds

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import dagger.Module
import dagger.Binds
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class IssueRefundModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: IssueRefundFragment): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(IssueRefundViewModel::class)
    abstract fun bindFactory(factory: IssueRefundViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(activity: MainActivity): SavedStateRegistryOwner
}
