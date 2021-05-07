package com.woocommerce.android.ui.login

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.LoginAnalyticsListener

@InstallIn(SingletonComponent::class)
@Module
class LoginAnalyticsModule {
    @Provides
    fun provideAnalyticsListener(
        accountStore: AccountStore,
        siteStore: SiteStore,
        unifiedLoginTracker: UnifiedLoginTracker
    ): LoginAnalyticsListener {
        return LoginAnalyticsTracker(accountStore, siteStore, unifiedLoginTracker)
    }
}
