package com.woocommerce.android.di

import android.content.Context
import android.webkit.CookieManager
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.WooLog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import java.util.Locale
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppConfigModule {
    companion object {
        private const val USER_AGENT_APPNAME = "wc-android"
    }

    @Provides
    fun provideAppSecrets() = AppSecrets(BuildConfig.OAUTH_APP_ID, BuildConfig.OAUTH_APP_SECRET)

    @Provides
    @Singleton
    fun provideUserAgent(
        appContext: Context,
        @AppCoroutineScope coroutineScope: CoroutineScope
    ) = UserAgent(appContext, USER_AGENT_APPNAME, coroutineScope)

    @Provides
    fun provideDefaultLocale(): Locale = Locale.getDefault()

    @Provides
    @Singleton
    fun providesAppPrefs(appContext: Context): AppPrefs {
        AppPrefs.init(appContext)
        return AppPrefs
    }

    @Provides
    @Singleton
    fun provideFeedbackPrefs(appContext: Context) = FeedbackPrefs(appContext)

    @Provides
    @Singleton
    fun provideStringUtils() = StringUtils

    @Provides
    fun provideWebViewCookieManager() = CookieManager.getInstance()

    @Provides
    @Singleton
    fun provideWooLog(context: Context): WooLog {
        WooLog.init(context)
        return WooLog
    }
}
