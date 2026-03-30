package com.woocommerce.android.di

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.util.WooLog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import org.wordpress.android.fluxc.network.HtmlResponseLoggingConfig
import org.wordpress.android.fluxc.network.HtmlResponseLoggingInterceptor
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class HtmlResponseLoggingModule {
    @Provides
    @Singleton
    fun provideHtmlResponseLoggingConfig(): HtmlResponseLoggingConfig {
        return object : HtmlResponseLoggingConfig {
            override val isEnabled: Boolean
                get() = AppPrefs.isAdvancedHtmlErrorLoggingEnabled()

            override fun onHtmlResponseDetected(
                endpoint: String,
                statusCode: Int,
                contentType: String?,
                bodyPreview: String,
                redirectTarget: String?
            ) {
                val message = buildString {
                    appendLine("[HTML Response Detected]")
                    appendLine("  Endpoint: $endpoint")
                    appendLine("  Status: $statusCode")
                    appendLine("  Content-Type: $contentType")
                    appendLine("  Body preview: $bodyPreview")
                    append("  Redirect: ${redirectTarget ?: "(none)"}")
                }
                WooLog.w(WooLog.T.UTILS, message)
            }
        }
    }

    @Provides
    @IntoSet
    @Named("interceptors")
    fun provideHtmlResponseLoggingInterceptor(
        config: HtmlResponseLoggingConfig
    ): Interceptor = HtmlResponseLoggingInterceptor(config)
}
