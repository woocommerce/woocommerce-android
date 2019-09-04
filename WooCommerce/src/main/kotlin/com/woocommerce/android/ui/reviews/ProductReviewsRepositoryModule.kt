package com.woocommerce.android.ui.reviews

import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
internal abstract class ProductReviewsRepositoryModule {
    @Binds
    @Singleton
    internal abstract fun provideProductReviewsRepository(
        productReviewsRepo: ProductReviewsRepository
    ): ProductReviewsRepositoryContract
}
