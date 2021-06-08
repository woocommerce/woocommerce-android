package com.woocommerce.android.ui.refunds

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.refunds.RefundsModule.RefundDetailFragmentModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    RefundDetailFragmentModule::class
])
object RefundsModule {
    @Module
    abstract class RefundDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [RefundDetailModule::class])
        abstract fun refundDetailFragment(): RefundDetailFragment
    }
}
