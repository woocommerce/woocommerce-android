package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

interface ApplicationPasswordsConfiguration {
    val isEnabledForSiteCredentials: Boolean
    val isEnabledForJetpack: Boolean
    val applicationName: String
}
