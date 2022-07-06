package com.woocommerce.android.di

import com.woocommerce.android.ui.login.replacements.WooLoginEmailFragment
import com.woocommerce.android.ui.login.replacements.WooLoginSiteAddressFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class LoginFragmentsModule {
    @ContributesAndroidInjector
    abstract fun provideWooLoginSiteAddressFragment(): WooLoginSiteAddressFragment

    @ContributesAndroidInjector
    abstract fun provideWooLoginEmailFragment(): WooLoginEmailFragment
}
