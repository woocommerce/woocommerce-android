package com.woocommerce.android.ui.reviews

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ReviewListModule {
    @Binds
    abstract fun provideProductReviewsRepository(
        productReviewsRepository: ProductReviewsRepository
    ): ProductReviewsRepositoryContract

    @ContributesAndroidInjector
    abstract fun reviewListFragment(): ReviewListFragment
}
