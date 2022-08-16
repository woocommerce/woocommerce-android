package com.woocommerce.android.ui.sitediscovery

import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
interface SiteDiscoveryModule {
    @ContributesAndroidInjector
    fun providePostLoginSitedAddressFragment(): PostLoginSitedAddressFragment
}
