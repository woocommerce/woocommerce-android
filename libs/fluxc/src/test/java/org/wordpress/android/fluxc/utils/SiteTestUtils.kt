package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.SiteModel

fun generateWPComSite() = SiteModel().apply {
    url = ""
    xmlRpcUrl = ""
    siteId = 556
    setIsWPCom(true)
    setIsVisible(true)
    origin = SiteModel.ORIGIN_WPCOM_REST
}
