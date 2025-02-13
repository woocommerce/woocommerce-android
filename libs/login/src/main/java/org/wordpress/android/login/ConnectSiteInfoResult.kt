package org.wordpress.android.login

data class ConnectSiteInfoResult @JvmOverloads constructor(
    val url: String,
    val urlAfterRedirects: String?,
    val hasJetpack: Boolean,
    /**
     * Whether the site is suspended on WordPress.com and can't be connected using Jetpack
     */
    val isWPComSuspended: Boolean = false,
)
