package org.wordpress.android.login

data class ConnectSiteInfoResult @JvmOverloads constructor(
    val url: String,
    val urlAfterRedirects: String?,
    val hasJetpack: Boolean,
    /**
     * Whether the site is suspended on WordPress.com and can't be connected using Jetpack
     */
    val isWPComSuspended: Boolean = false,
    val isWPCom: Boolean = false,
    val isCommerceGarden: Boolean = false,
    val isJetpackConnected: Boolean = false,
) {
    /**
     * Whether the site should authenticate through the WordPress.com login (email) flow rather than
     * site credentials. Mirrors the iOS routing in
     * `AuthenticationManager.shouldPresentUsernamePasswordController` so both platforms send
     * WordPress.com, Commerce-garden and Jetpack-connected sites to the email screen, and only
     * self-hosted sites without a Jetpack connection to the site-credentials screen.
     */
    val shouldUseWPComLogin: Boolean
        get() = isWPCom || isCommerceGarden || isJetpackConnected
}
