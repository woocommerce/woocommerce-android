package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject

/**
 * True when the store runs a WooCommerce core version BELOW the one that ships the catalog
 * file-access fix (11.0.0). Used to decide how to react to a blocked catalog file: below the
 * fix version we silently fall back to the legacy remote sync, at or above it we surface the
 * "contact your hosting provider" screen (the fix exists, so a still-blocked file is a host issue).
 *
 * Unknown version resolves to `false` (surface the screen) so we never silently degrade a store
 * we can't classify.
 */
class WooPosIsWooBelowCatalogFixVersion @Inject constructor(
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
    private val fetchWooVersion: FetchActiveWCPluginVersion,
) {
    suspend operator fun invoke(): Boolean {
        val version = getWooCoreVersion() ?: fetchWooVersion() ?: return false
        return version.semverCompareTo(WC_CATALOG_FIX_VERSION) < 0
    }

    companion object {
        const val WC_CATALOG_FIX_VERSION = "11.0.0"
    }
}
