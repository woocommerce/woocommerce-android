package org.wordpress.android.fluxc.network.rest.wpcom.wc

/**
 * Notified when a WooCommerce REST request fails because WPCom no longer recognises the site ID
 * (the `unknown_blog` error code). This usually means the selected site is stale, was disconnected
 * from Jetpack, or was deleted, and the app should recover from it.
 */
interface UnknownBlogListener {
    fun onUnknownBlog(siteId: Long)
}
