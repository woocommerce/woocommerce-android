package com.woocommerce.android.di

import com.facebook.stetho.okhttp3.StethoInterceptor
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.util.AppLog
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

// TODO Replace this with version from FluxC when that's available
@Module
class DebugOkHttpClientModule {
    @Provides
    @Named("regular")
    fun provideOkHttpClientBuilder(): OkHttpClient.Builder =
            OkHttpClient.Builder().addNetworkInterceptor(StethoInterceptor())

    @Provides
    @Named("custom-ssl")
    fun provideOkHttpClientBuilderCustomSSL(memorizingTrustManager: MemorizingTrustManager): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(memorizingTrustManager), SecureRandom())
            val sslSocketFactory = sslContext.socketFactory
            builder.sslSocketFactory(sslSocketFactory)
        } catch (e: NoSuchAlgorithmException) {
            AppLog.e(AppLog.T.API, e)
        } catch (e: KeyManagementException) {
            AppLog.e(AppLog.T.API, e)
        }

        builder.addNetworkInterceptor(StethoInterceptor())
        return builder
    }

    @Singleton
    @Provides
    @Named("custom-ssl")
    fun provideMediaOkHttpClientInstanceCustomSSL(@Named("custom-ssl") builder: OkHttpClient.Builder): OkHttpClient {
        return builder
                .connectTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .build()
    }

    @Singleton
    @Provides
    @Named("regular")
    fun provideMediaOkHttpClientInstance(@Named("regular") builder: OkHttpClient.Builder): OkHttpClient {
        return builder
                .connectTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(BaseRequest.DEFAULT_REQUEST_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .build()
    }
}
