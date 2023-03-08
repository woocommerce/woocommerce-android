package com.woocommerce.android.di

import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.support.ZendeskManager
import com.woocommerce.android.support.ZendeskSettings
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Singleton
import org.wordpress.android.fluxc.store.AccountStore

@InstallIn(SingletonComponent::class)
@Module
class SupportModule {
    @Singleton
    @Provides
    fun provideZendeskSettings(
        supportHelper: SupportHelper,
        accountStore: AccountStore,
        selectedSite: SelectedSite
    ) : ZendeskSettings = ZendeskSettings(supportHelper, accountStore, selectedSite)

    @Singleton
    @Provides
    fun provideZendeskManager(
        zendeskSettings: ZendeskSettings,
        siteStore: SiteStore,
        dispatchers: CoroutineDispatchers
    ): ZendeskManager = ZendeskManager(zendeskSettings, siteStore, dispatchers)

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()
}
