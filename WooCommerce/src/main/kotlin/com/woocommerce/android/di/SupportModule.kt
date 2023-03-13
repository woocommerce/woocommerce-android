package com.woocommerce.android.di

import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.support.ZendeskHelper
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
    fun provideZendeskHelper(
        siteStore: SiteStore,
        supportHelper: SupportHelper,
        accountStore: AccountStore,
        selectedSite: SelectedSite,
        dispatchers: CoroutineDispatchers
    ): ZendeskHelper = ZendeskHelper(siteStore, supportHelper, accountStore, selectedSite, dispatchers)

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()
}
