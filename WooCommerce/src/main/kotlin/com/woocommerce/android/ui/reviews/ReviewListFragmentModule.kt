package com.woocommerce.android.ui.reviews

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ReviewListFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [ReviewListModule::class])
    abstract fun reviewListFragment(): ReviewListFragment
}
