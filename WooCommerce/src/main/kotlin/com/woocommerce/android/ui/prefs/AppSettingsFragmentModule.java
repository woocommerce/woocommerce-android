package com.woocommerce.android.ui.prefs;

import com.woocommerce.android.di.FragmentScope;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class AppSettingsFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    abstract AppSettingsFragment appSettingsFragment();
}
