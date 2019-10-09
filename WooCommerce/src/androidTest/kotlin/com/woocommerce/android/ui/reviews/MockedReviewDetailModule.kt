package com.woocommerce.android.ui.reviews

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MockedReviewDetailModule {
    @ContributesAndroidInjector
    abstract fun reviewDetailFragment(): ReviewDetailFragment
}
