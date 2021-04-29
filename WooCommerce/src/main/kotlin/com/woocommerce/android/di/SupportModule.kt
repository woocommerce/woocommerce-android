package com.woocommerce.android.di

import android.content.Context
import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.support.ZendeskHelper
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
        context: Context,
        accountStore: AccountStore,
        siteStore: SiteStore,
        supportHelper: SupportHelper
    ): ZendeskHelper = ZendeskHelper(context, accountStore, siteStore, supportHelper)

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()
}
