package com.woocommerce.android.ui.jitm

import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse

interface JitmBannerMessageProvider {
    suspend fun getMessagesForPath(messagePath: String): List<JITMApiResponse>
    suspend fun dismissMessage(messagePath: String, jitmId: String, featureClass: String): WooResult<Boolean>
    fun onCtaClicked(messagePath: String, jitmId: String)
}
