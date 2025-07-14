package org.wordpress.android.fluxc.model

data class WCSSRModel(
    val remoteSiteId: Long,
    val environment: String? = null,
    val database: String? = null,
    val activePlugins: String? = null,
    val theme: String? = null,
    val settings: String? = null,
    val security: String? = null,
    val pages: String? = null
)
