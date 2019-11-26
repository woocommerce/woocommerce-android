package com.woocommerce.android.ui.reviews

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MockedReviewDetailFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [MockedReviewDetailModule::class])
    abstract fun reviewDetailFragment(): ReviewDetailFragment
}
