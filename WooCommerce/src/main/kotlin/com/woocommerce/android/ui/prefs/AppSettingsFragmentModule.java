package com.woocommerce.android.ui.prefs;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class AppSettingsFragmentModule {
    @ContributesAndroidInjector
    abstract AppSettingsFragment appSettingsFragment();
}
