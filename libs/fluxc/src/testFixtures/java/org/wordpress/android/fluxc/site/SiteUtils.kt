@file:Suppress("MagicNumber")
package org.wordpress.android.fluxc.site

import org.wordpress.android.fluxc.model.SiteModel

object SiteUtils {
    fun generateWPComSite(): SiteModel = generateTestSite(556, "", true)

    fun generateTestSite(remoteId: Long, url: String, isWPCom: Boolean): SiteModel =
        SiteModel().apply {
            setUrl(url)
            siteId = remoteId
            setIsWPCom(isWPCom)
            origin = if (isWPCom) SiteModel.ORIGIN_WPCOM_REST else SiteModel.ORIGIN_XMLRPC
        }

    fun generateSelfHostedNonJPSite(): SiteModel = SiteModel().apply {
        selfHostedSiteId = 6
        setIsWPCom(false)
        setIsJetpackInstalled(false)
        setIsJetpackConnected(false)
        setUrl("http://some.url")
        origin = SiteModel.ORIGIN_XMLRPC
    }

    fun generateJetpackSiteOverXMLRPC(): SiteModel = SiteModel().apply {
        siteId = 982
        selfHostedSiteId = 8
        setIsWPCom(false)
        setIsJetpackInstalled(true)
        setIsJetpackConnected(true)
        username = "ponyuser"
        password = "ponypass"
        setUrl("http://jetpack.url")
        origin = SiteModel.ORIGIN_XMLRPC
    }

    fun generateJetpackSiteOverRestOnly(): SiteModel = SiteModel().apply {
        siteId = 5623
        setIsWPCom(false)
        setIsJetpackInstalled(true)
        setIsJetpackConnected(true)
        setUrl("http://jetpack2.url")
        origin = SiteModel.ORIGIN_WPCOM_REST
    }

    fun generateSelfHostedSiteFutureJetpack(): SiteModel = SiteModel().apply {
        selfHostedSiteId = 8
        setIsWPCom(false)
        setIsJetpackInstalled(false)
        setIsJetpackConnected(false)
        setUrl("http://jetpack2.url")
        origin = SiteModel.ORIGIN_XMLRPC
    }
}
