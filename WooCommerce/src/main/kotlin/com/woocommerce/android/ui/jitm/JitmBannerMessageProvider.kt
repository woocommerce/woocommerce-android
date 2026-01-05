package com.woocommerce.android.ui.jitm

import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse

interface JitmBannerMessageProvider {
    suspend fun getMessagesForPath(messagePath: String): List<JITMApiResponse>
    suspend fun dismissMessage(messagePath: String, jitmId: String, featureClass: String): Boolean
    fun onCtaClicked(messagePath: String, jitmId: String)
}
