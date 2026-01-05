package com.woocommerce.android.ui.jitm

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jitm.clientside.ClientSideJitmBannerProvider
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitmBannerMessageRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val jitmBannerAdapter: JitmBannerAdapter,
    private val clientSideBannerProvider: ClientSideJitmBannerProvider,
) : JitmBannerMessageProvider {

    override suspend fun getMessagesForPath(messagePath: String): List<JITMApiResponse> {
        return getProvider().getMessagesForPath(messagePath)
    }

    override suspend fun dismissMessage(messagePath: String, jitmId: String, featureClass: String): Boolean {
        return getProvider().dismissMessage(messagePath, jitmId, featureClass)
    }

    override fun onCtaClicked(messagePath: String) {
        getProvider().onCtaClicked(messagePath)
    }

    private fun getProvider(): JitmBannerMessageProvider {
        val site = selectedSite.getIfExists()
        return if (site != null && site.isJetpackConnected) {
            jitmBannerAdapter
        } else {
            clientSideBannerProvider
        }
    }
}
