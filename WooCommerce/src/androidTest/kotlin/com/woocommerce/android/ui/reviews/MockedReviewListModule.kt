package com.woocommerce.android.ui.reviews

import com.woocommerce.android.model.ProductReview
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MockedReviewListModule {
    @Module
    companion object {
        private var reviews: List<ProductReview>? = null

        fun setMockReviews(reviewsList: List<ProductReview>) {
            this.reviews = reviewsList
        }
    }

    @ContributesAndroidInjector
    abstract fun reviewListFragment(): ReviewListFragment
}
