package com.woocommerce.android.ui.jitm.clientside

import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse

interface ClientSideBanner {
    val messagePath: String
    val bannerId: String

    fun shouldShow(): Boolean
    fun toJitmResponse(): JITMApiResponse
    fun onDismiss()
    fun onCtaClick()
}
