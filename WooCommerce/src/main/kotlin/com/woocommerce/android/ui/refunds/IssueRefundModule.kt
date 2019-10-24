package com.woocommerce.android.ui.refunds

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.Binds
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class IssueRefundModule {
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
    @ViewModelKey(IssueRefundViewModel::class)
    abstract fun bindFactory(factory: IssueRefundViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: IssueRefundFragment): SavedStateRegistryOwner

    @ContributesAndroidInjector
    abstract fun refundByAmountFragment(): RefundByAmountFragment

    @ContributesAndroidInjector
    abstract fun refundSummaryFragment(): RefundSummaryFragment

}
