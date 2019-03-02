package com.woocommerce.android.util

/**
 * Simple HashMap of product remoteId/imageUrl used for quick lookups when attempting to display prodcut images.
 * Note that this does *not* store the siteId so it must be cleared when the site is changed
 */
object ProductImageUrlMap : HashMap<Long, String>()
