package com.woocommerce.android.ui.refunds

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class RefundByAmountFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [IssueRefundModule::class])
    abstract fun refundByAmountFragment(): RefundByAmountFragment
}
