package com.woocommerce.android.ui.reviews

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ReviewDetailFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [ReviewDetailModule::class])
    abstract fun reviewDetailFragment(): ReviewDetailFragment
}
