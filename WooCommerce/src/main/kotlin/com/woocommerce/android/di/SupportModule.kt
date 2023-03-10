package com.woocommerce.android.di

import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.support.zendesk.ZendeskDeviceDataSource
import com.woocommerce.android.support.zendesk.ZendeskManager
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class SupportModule {
    @Singleton
    @Provides
    fun provideZendeskSettings(
        supportHelper: SupportHelper,
        accountStore: AccountStore,
        selectedSite: SelectedSite
    ): ZendeskSettings = ZendeskSettings(supportHelper, accountStore, selectedSite)

    @Singleton
    @Provides
    fun provideZendeskManager(
        zendeskSettings: ZendeskSettings,
        deviceDataSource: ZendeskDeviceDataSource,
        siteStore: SiteStore,
        dispatchers: CoroutineDispatchers
    ): ZendeskManager = ZendeskManager(zendeskSettings, deviceDataSource, siteStore, dispatchers)

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()
}
