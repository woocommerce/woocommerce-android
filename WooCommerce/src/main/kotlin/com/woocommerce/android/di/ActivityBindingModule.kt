package com.woocommerce.android.di

import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBindingModule {
    @ContributesAndroidInjector(modules = arrayOf(MainModule::class))
    abstract fun provideMainActivityInjector(): MainActivity
}
