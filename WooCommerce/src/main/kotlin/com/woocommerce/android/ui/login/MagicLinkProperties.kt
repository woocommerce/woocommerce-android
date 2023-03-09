package com.woocommerce.android.ui.login

import org.wordpress.android.fluxc.store.AccountStore.AuthEmailFlow
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailSource

enum class MagicLinkSource(private val value: String) : AuthEmailSource {
    JetpackInstallation("jetpack-installation"),
    JetpackConnection("jetpack-connection"),
    WPComAuthentication("wpcom-authentication");

    override fun getName(): String = value
}

enum class MagicLinkFlow(private val value: String) : AuthEmailFlow {
    SiteCredentialsToWPCom("sitecredentials-to-wpcom");

    override fun getName(): String = value
}
