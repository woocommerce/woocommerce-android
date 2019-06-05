package com.woocommerce.android.ui.notifications

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ReviewDetailModule {
    @Binds
    abstract fun provideReviewDetailPresenter(
        reviewDetailPresenter: ReviewDetailPresenter
    ): ReviewDetailContract.Presenter

    @ContributesAndroidInjector
    abstract fun reviewDetailFragment(): ReviewDetailFragment
}
