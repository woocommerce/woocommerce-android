package com.woocommerce.android.ui.prefs

import com.woocommerce.android.di.FragmentScope

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class MainSettingsModule {
    @FragmentScope
    @Binds
    abstract fun provideMainSettingsPresenter(
        mainSettingsPresenter: MainSettingsPresenter): MainSettingsContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun mainSettingsFragment(): MainSettingsFragment
}
