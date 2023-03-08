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
    fun provideZendeskProvider() : ZendeskSettings = ZendeskSettings()

    @Singleton
    @Provides
    fun provideZendeskHelper(
        zendeskSettings: ZendeskSettings,
        siteStore: SiteStore,
        supportHelper: SupportHelper,
        accountStore: AccountStore,
        selectedSite: SelectedSite,
        dispatchers: CoroutineDispatchers
    ): ZendeskManager = ZendeskManager(
        zendeskSettings,
        siteStore,
        supportHelper,
        accountStore,
        selectedSite,
        dispatchers
    )

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()
}
