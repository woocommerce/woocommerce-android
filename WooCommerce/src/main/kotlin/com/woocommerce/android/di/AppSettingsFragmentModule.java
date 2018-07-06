package com.woocommerce.android.di;

import com.woocommerce.android.ui.prefs.AppSettingsFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class AppSettingsFragmentModule {
    @ContributesAndroidInjector
    abstract AppSettingsFragment appSettingsFragment();
}
