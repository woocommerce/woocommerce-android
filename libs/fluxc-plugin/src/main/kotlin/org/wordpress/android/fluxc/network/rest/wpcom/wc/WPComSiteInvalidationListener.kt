package org.wordpress.android.fluxc.network.rest.wpcom.wc

/**
 * Notified when WPCom indicates that a site can no longer be used by the app.
 */
interface WPComSiteInvalidationListener {
    fun onSiteInvalidated(event: WPComSiteInvalidationEvent)
}

data class WPComSiteInvalidationEvent(
    val siteId: Long,
    val reason: WPComSiteInvalidationReason
)

enum class WPComSiteInvalidationReason {
    UNKNOWN_BLOG
}
