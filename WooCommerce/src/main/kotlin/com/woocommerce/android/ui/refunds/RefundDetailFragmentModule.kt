package com.woocommerce.android.ui.refunds

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class RefundDetailFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [RefundDetailModule::class])
    abstract fun refundDetailFragment(): RefundDetailFragment
}
