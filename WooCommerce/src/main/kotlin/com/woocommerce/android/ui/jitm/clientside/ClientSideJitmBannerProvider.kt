package com.woocommerce.android.ui.jitm.clientside

import com.woocommerce.android.ui.jitm.JitmBannerMessageProvider
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientSideJitmBannerProvider @Inject constructor(
    private val posBanner: ClientSidePosBanner,
) : JitmBannerMessageProvider {

    override suspend fun getMessagesForPath(messagePath: String): List<JITMApiResponse> {
        return if (posBanner.messagePath == messagePath && posBanner.shouldShow()) {
            listOf(posBanner.toJitmResponse())
        } else {
            emptyList()
        }
    }

    override suspend fun dismissMessage(messagePath: String, jitmId: String, featureClass: String): Boolean {
        if (posBanner.bannerId == jitmId) {
            posBanner.onDismiss()
        }
        return true
    }

    override fun onCtaClicked(messagePath: String, jitmId: String) {
        if (posBanner.bannerId == jitmId) {
            posBanner.onCtaClick()
        }
    }
}
