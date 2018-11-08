package com.woocommerce.android.di

import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.support.ZendeskHelper
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Singleton

@Module
class SupportModule {
    @Singleton
    @Provides
    fun provideZendeskHelper(
        accountStore: AccountStore,
        siteStore: SiteStore,
        supportHelper: SupportHelper
    ): ZendeskHelper = ZendeskHelper(accountStore, siteStore, supportHelper)

    @Singleton
    @Provides
    fun provideSupportHelper(): SupportHelper = SupportHelper()
}
