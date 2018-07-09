package com.woocommerce.android.ui.prefs

import com.woocommerce.android.di.FragmentScope

import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class MainSettingsFragmentModule {
    @FragmentScope
    @Binds
    abstract fun provideMainSettingsFragmentPresenter(
        mainSettingsFragmentPresenter: MainSettingsFragmentPresenter): MainSettingsFragmentContract.Presenter

    @FragmentScope
    @ContributesAndroidInjector
    abstract fun mainSettingsFragment(): MainSettingsFragment
}
