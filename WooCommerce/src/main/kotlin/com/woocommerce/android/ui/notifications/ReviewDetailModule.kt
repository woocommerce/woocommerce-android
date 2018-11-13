package com.woocommerce.android.ui.notifications

import com.woocommerce.android.di.FragmentScope
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ReviewDetailModule {
    @FragmentScope
    @Binds
    abstract fun provideReviewDetailPresenter(
        reviewDetailPresenter: ReviewDetailPresenter
    ): ReviewDetailContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun reviewDetailFragment(): ReviewDetailFragment
}
