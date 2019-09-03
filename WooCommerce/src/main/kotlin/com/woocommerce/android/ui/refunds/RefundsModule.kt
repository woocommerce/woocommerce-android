package com.woocommerce.android.ui.refunds

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class RefundsModule {
    @ContributesAndroidInjector
    abstract fun issueRefundFragment(): IssueRefundFragment

    @ContributesAndroidInjector
    abstract fun refundByAmountFragment(): RefundByAmountFragment

    @ContributesAndroidInjector
    abstract fun refundConfirmationFragment(): RefundConfirmationFragment
}
