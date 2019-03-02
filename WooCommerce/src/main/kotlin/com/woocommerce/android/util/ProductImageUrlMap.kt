package com.woocommerce.android.util

/**
 * Simple map of product remoteId/imageUrl used for quick lookups when attempting to display product images.
 * Note that this does *not* store the siteId so it must be cleared when the site is changed
 */
object ProductImageUrlMap {
    private val map by lazy {
        HashMap<Long, String>()
    }

    fun get(remoteProductId: Long) = map[remoteProductId]

    fun put(remoteProductId: Long, imageUrl: String?) {
        imageUrl?.let  {
            map.put(remoteProductId, it)
        }
    }

    fun clear() {
        map.clear()
    }

    fun contains(remoteProductId: Long) = map.containsKey(remoteProductId)
}

