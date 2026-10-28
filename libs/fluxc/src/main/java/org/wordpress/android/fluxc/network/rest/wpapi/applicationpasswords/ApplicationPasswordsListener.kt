package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError

enum class ApplicationPasswordGenerationScenario {
    GENERATION, // creating a password when none is saved, for example at login
    REGENERATION // replacing the saved password because the store rejected it
}

interface ApplicationPasswordsListener {
    fun onNewPasswordCreated(scenario: ApplicationPasswordGenerationScenario) {}
    fun onPasswordGenerationFailed(
        networkError: WPAPINetworkError,
        path: String,
        scenario: ApplicationPasswordGenerationScenario
    ) {}
    fun onFeatureUnavailable(
        siteModel: SiteModel,
        networkError: WPAPINetworkError,
        path: String,
        scenario: ApplicationPasswordGenerationScenario
    ) {}
    fun onJetpackSiteFlaggedAsUnsupported(event: JetpackSiteFlaggedAsUnsupported)
}
