package com.woocommerce.android.ui.reviews

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ReviewListModule {
    @ContributesAndroidInjector
    abstract fun reviewListFragment(): ReviewListFragment
}
