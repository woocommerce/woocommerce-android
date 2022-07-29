package com.woocommerce.android.di

import android.net.Uri
import com.automattic.android.tracks.crashlogging.CrashLoggingOkHttpInterceptorProvider
import com.automattic.android.tracks.crashlogging.RequestFormatter
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Named
import okhttp3.Interceptor
import okhttp3.Request

@InstallIn(SingletonComponent::class)
@Module
class InterceptorModule {
    @Provides
    @IntoSet
    @Named("network-interceptors")
    fun provideNetworkInterceptor(): Interceptor = FlipperOkhttpInterceptor(
        AndroidFlipperClient.getInstanceIfInitialized()?.getPlugin(NetworkFlipperPlugin.ID)
    )

    @Provides
    @IntoSet
    @Named("network-interceptors")
    fun provideMonitoring(): Interceptor = CrashLoggingOkHttpInterceptorProvider.createInstance(WooRequestFormatter)
}

object WooRequestFormatter : RequestFormatter {
    override fun formatRequestUrl(request: Request): String {

        val obfustactedRequest = request.newBuilder()

        val newUrl = request.url.newBuilder().query(null)
        when {
            request.url.pathSegments.contains("rest-api") -> {
                request.url.queryParameterNames.forEach {
                    if (it == "_method" || it == "path") {
                        newUrl.addQueryParameter(it, request.url.queryParameter(it))
                    }
                }
            }
        }

        request.url.pathSegments.forEachIndexed { index, pathSegment ->
            if (pathSegment.matches(Regex("\\d{6,9}\$"))) {
                newUrl.setPathSegment(index, "<blog_id>")
            }
        }

        if (request.url.host == "public-api.wordpress.com") {
            newUrl.host("wp_api")
        }
        obfustactedRequest.url(newUrl.build())

        return Uri.decode(obfustactedRequest.build().url.toString())
    }
}
