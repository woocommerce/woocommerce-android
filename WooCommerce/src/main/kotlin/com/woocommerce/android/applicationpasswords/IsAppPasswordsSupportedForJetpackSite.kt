package com.woocommerce.android.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport
import javax.inject.Inject

class IsAppPasswordsSupportedForJetpackSite @Inject constructor(
    private val applicationPasswordsConfiguration: ApplicationPasswordsConfiguration,
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport
) {
    suspend operator fun invoke(site: SiteModel): Boolean {
        return applicationPasswordsConfiguration.isEnabledForJetpackAccess() &&
            jetpackApplicationPasswordsSupport.supportsAppPasswords(site)
    }
}
