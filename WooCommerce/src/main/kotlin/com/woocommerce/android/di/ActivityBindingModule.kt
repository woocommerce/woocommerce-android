package com.woocommerce.android.di

import com.woocommerce.android.ui.main.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBindingModule {
    @ContributesAndroidInjector
    abstract fun provideMainActivityInjector(): MainActivity
}
