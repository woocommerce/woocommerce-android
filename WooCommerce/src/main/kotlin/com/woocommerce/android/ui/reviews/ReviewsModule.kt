package com.woocommerce.android.ui.reviews

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.reviews.ReviewsModule.ReviewDetailFragmentModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    ReviewDetailFragmentModule::class
])
object ReviewsModule {
    @Module
    internal abstract class ReviewDetailFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ReviewDetailModule::class])
        abstract fun reviewDetailFragment(): ReviewDetailFragment
    }
}
