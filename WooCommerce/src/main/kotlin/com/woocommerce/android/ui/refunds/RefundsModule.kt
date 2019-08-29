package com.woocommerce.android.ui.refunds

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class RefundsModule {
    @ContributesAndroidInjector
    abstract fun refundfragment(): RefundsFragment

    @ContributesAndroidInjector
    abstract fun refundByAmountfragment(): RefundByAmountFragment
}
