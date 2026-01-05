package com.woocommerce.android.ui.jitm.clientside

import com.woocommerce.android.ui.jitm.JitmBannerMessageProvider
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientSideJitmBannerProvider @Inject constructor(
    private val banners: Set<@JvmSuppressWildcards ClientSideBanner>,
) : JitmBannerMessageProvider {

    override suspend fun getMessagesForPath(messagePath: String): List<JITMApiResponse> {
        return banners
            .filter { it.messagePath == messagePath && it.shouldShow() }
            .map { it.toJitmResponse() }
    }

    override suspend fun dismissMessage(messagePath: String, jitmId: String, featureClass: String): Boolean {
        val banner = banners.find { it.bannerId == jitmId }
        banner?.onDismiss()
        return true
    }

    override fun onCtaClicked(messagePath: String) {
        banners
            .filter { it.messagePath == messagePath }
            .forEach { it.onCtaClick() }
    }
}
