@file:Suppress("ImportOrdering")

package com.woocommerce.android.aiassistant.headless

import android.content.Context
import com.android.volley.ExecutorDelivery
import com.android.volley.Network
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import kotlinx.coroutines.Dispatchers
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.module.ApplicationPasswordsModule
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.fluxc.network.OkHttpStack
import org.wordpress.android.fluxc.network.OpenJdkCookieManager
import org.wordpress.android.fluxc.network.RetryOnRedirectBasicNetwork
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalse
import org.wordpress.android.fluxc.network.rest.JsonObjectOrFalseDeserializer
import org.wordpress.android.fluxc.network.rest.JsonObjectOrNullAdapterFactory
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.util.concurrent.Executor
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@DisableInstallInCheck
@Module(includes = [ApplicationPasswordsModule::class])
object WooAiSmokeRobolectricNetworkModule {
    @Provides
    @Singleton
    @Named("regular")
    fun provideRequestQueue(
        @Named("regular") okHttpClient: OkHttpClient,
        context: Context,
    ): RequestQueue = newRequestQueue(okHttpClient, context)

    @Provides
    @Singleton
    @Named("no-redirects")
    fun provideNoRedirectsRequestQueue(
        @Named("no-redirects") okHttpClient: OkHttpClient,
        context: Context,
    ): RequestQueue = newRetryOnRedirectRequestQueue(okHttpClient, context)

    @Provides
    @Singleton
    @Named("custom-ssl")
    fun provideRequestQueueCustomSsl(
        @Named("custom-ssl") okHttpClient: OkHttpClient,
        context: Context,
    ): RequestQueue = newRequestQueue(okHttpClient, context)

    @Provides
    @Singleton
    @Named("custom-ssl-custom-redirects")
    fun provideRequestQueueCustomSslWithRedirects(
        @Named("custom-ssl-custom-redirects") okHttpClient: OkHttpClient,
        context: Context,
    ): RequestQueue = newRequestQueue(okHttpClient, context)

    @Provides
    @Singleton
    @Named("no-cookies")
    fun provideRequestQueueNoCookies(
        @Named("no-cookies") okHttpClient: OkHttpClient,
        context: Context,
    ): RequestQueue = newRequestQueue(okHttpClient, context)

    @Provides
    @Singleton
    fun provideMemorizingTrustManager(): MemorizingTrustManager = MemorizingTrustManager()

    @Provides
    @Singleton
    fun provideCookieManager(): CookieManager {
        val cookieManager = OpenJdkCookieManager()
        CookieHandler.setDefault(cookieManager)
        return cookieManager
    }

    @Provides
    @Singleton
    fun provideCookieJar(cookieManager: CookieManager): CookieJar = JavaNetCookieJar(cookieManager)

    @Provides
    @Singleton
    fun provideCoroutineContext(): CoroutineContext = Dispatchers.Default

    @Provides
    @Singleton
    @Suppress("DEPRECATION")
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .registerTypeHierarchyAdapter(JsonObjectOrFalse::class.java, JsonObjectOrFalseDeserializer())
        .registerTypeAdapterFactory(JsonObjectOrNullAdapterFactory())
        .create()

    private fun newRetryOnRedirectRequestQueue(
        okHttpClient: OkHttpClient,
        context: Context,
    ): RequestQueue = createRequestQueue(RetryOnRedirectBasicNetwork(OkHttpStack(okHttpClient)), context)

    private fun newRequestQueue(
        okHttpClient: OkHttpClient,
        context: Context,
    ): RequestQueue = createRequestQueue(BasicNetwork(OkHttpStack(okHttpClient)), context)

    private fun createRequestQueue(
        network: Network,
        context: Context,
    ): RequestQueue {
        val cacheDir = File(context.cacheDir, DEFAULT_CACHE_DIR)
        val delivery = ExecutorDelivery(Executor { command -> command.run() })
        return RequestQueue(DiskBasedCache(cacheDir), network, NETWORK_THREAD_POOL_SIZE, delivery).apply {
            start()
        }
    }

    private const val DEFAULT_CACHE_DIR = "volley-fluxc"
    private const val NETWORK_THREAD_POOL_SIZE = 10
}
