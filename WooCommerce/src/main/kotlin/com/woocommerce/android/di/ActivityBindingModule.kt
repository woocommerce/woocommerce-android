package com.woocommerce.android.di

import com.woocommerce.android.ui.login.MagicLinkInterceptActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class ActivityBindingModule {
    @ActivityScope
    @ContributesAndroidInjector
    abstract fun provideMagicLinkInterceptActivityInjector(): MagicLinkInterceptActivity
}
