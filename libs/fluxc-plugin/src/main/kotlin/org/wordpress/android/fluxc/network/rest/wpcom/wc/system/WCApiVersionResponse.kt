package org.wordpress.android.fluxc.network.rest.wpcom.wc.system

import org.wordpress.android.fluxc.model.SiteModel

data class WCApiVersionResponse(
    val siteModel: SiteModel,
    val apiVersion: String?
)
