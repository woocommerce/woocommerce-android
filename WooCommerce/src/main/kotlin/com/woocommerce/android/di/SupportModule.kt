package com.woocommerce.android.di

import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.support.zendesk.ZendeskTicketRepository
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class SupportModule {
    @Provides
    fun provideZendeskTicketRepository(
        zendeskSettings: ZendeskSettings,
        siteStore: SiteStore,
        dispatchers: CoroutineDispatchers
    ): ZendeskTicketRepository = ZendeskTicketRepository(
        zendeskSettings,
        ZendeskEnvironmentDataSource(),
        siteStore,
        dispatchers
    )

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()
}
