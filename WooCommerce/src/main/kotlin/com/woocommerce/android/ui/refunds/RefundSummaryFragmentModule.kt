package com.woocommerce.android.ui.refunds

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class RefundSummaryFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [IssueRefundModule::class])
    abstract fun refundSummaryFragment(): RefundSummaryFragment
}
