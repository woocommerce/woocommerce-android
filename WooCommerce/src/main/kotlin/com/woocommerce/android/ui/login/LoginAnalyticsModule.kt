package com.woocommerce.android.ui.login

import dagger.Module
import dagger.Provides
import org.wordpress.android.login.LoginAnalyticsListener

@Module
class LoginAnalyticsModule {
    @Provides
    fun provideAnalyticsListener(): LoginAnalyticsListener = LoginAnalyticsTracker()
}
