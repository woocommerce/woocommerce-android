package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

interface ApplicationPasswordsConfiguration {
    val applicationName: String

    /**
     * Returns true if the Application Passwords feature is enabled for direct site access.
     * This applies when sites are accessed using their URL directly (not through Jetpack).
     */
    fun isEnabledForDirectAccess(): Boolean = true

    /**
     * Returns true if the Application Passwords feature is enabled for Jetpack-mediated access.
     * This applies when sites are accessed through Jetpack, even if they are self-hosted.
     */
    suspend fun isEnabledForJetpackAccess(): Boolean
}
