package com.woocommerce.android.ui.login

enum class MagicLinkSource(val value: String) {
    JetpackInstallation("jetpack-installation"),
    JetpackConnection("jetpack-connection"),
    WPComAuthentication("wpcom-authentication\n")
}

enum class MagicLinkFlow(val value: String) {
    SiteCredentialsToWPCom("sitecredentials-to-wpcom")
}
