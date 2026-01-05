package com.woocommerce.android.ui.jitm

import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitmBannerAdapter @Inject constructor(
    private val jitmStoreInMemoryCache: JitmStoreInMemoryCache,
) : JitmBannerMessageProvider {

    override suspend fun getMessagesForPath(messagePath: String): List<JITMApiResponse> {
        return jitmStoreInMemoryCache.getMessagesForPath(messagePath)
    }

    override suspend fun dismissMessage(messagePath: String, jitmId: String, featureClass: String): Boolean {
        val result = jitmStoreInMemoryCache.dismissJitmMessage(messagePath, jitmId, featureClass)
        return result.model == true
    }

    override fun onCtaClicked(messagePath: String) {
        jitmStoreInMemoryCache.onCtaClicked(messagePath)
    }
}
