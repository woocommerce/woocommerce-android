package com.woocommerce.android.ui.reviews

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MockedReviewListFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [MockedReviewListModule::class])
    abstract fun reviewListFragment(): ReviewListFragment
}
