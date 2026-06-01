package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.auth.AccessTokenWpComOAuthTokenProvider
import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider
import com.woocommerce.android.aiassistant.chat.WooMobileAiChatService
import com.woocommerce.android.aiassistant.chat.WooMobileAiChatService.Companion.DEFAULT_BASE_URL
import com.woocommerce.android.aiassistant.core.chat.ChatService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.network.UserAgent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AssistantNetworkModule {
    @Binds
    @Singleton
    internal abstract fun bindChatService(impl: WooMobileAiChatService): ChatService

    @Binds
    @Singleton
    internal abstract fun bindWpComOAuthTokenProvider(impl: AccessTokenWpComOAuthTokenProvider): WpComOAuthTokenProvider

    companion object {
        private const val SSE_CALL_TIMEOUT_SECONDS = 60L

        /**
         * Derives an SSE-tuned `OkHttpClient` from the app-wide `@Named("regular")`
         * client. We disable the read timeout (long-lived stream) but keep a
         * call-level timeout as a hard ceiling so a hung connection cannot
         * leak indefinitely.
         */
        @Provides
        @Singleton
        @AssistantOkHttpClient
        internal fun provideAssistantOkHttpClient(
            @Named("regular") base: OkHttpClient,
            userAgent: UserAgent,
        ): OkHttpClient = base.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .callTimeout(SSE_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .cookieJar(CookieJar.NO_COOKIES)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", userAgent.apiUserAgent)
                    .build()
                chain.proceed(request)
            }
            .build()

        @Provides
        @AssistantBaseUrl
        internal fun provideAssistantBaseUrl(): String = DEFAULT_BASE_URL
    }
}
