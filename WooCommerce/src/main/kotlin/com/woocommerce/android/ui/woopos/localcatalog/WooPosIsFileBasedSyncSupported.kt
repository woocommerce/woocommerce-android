package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject

class WooPosIsFileBasedSyncSupported @Inject constructor(
    private val getWooVersion: GetWooCorePluginCachedVersion,
    private val fetchWooVersion: FetchActiveWCPluginVersion,
) {
    suspend operator fun invoke(): Boolean = supports(getWooVersion() ?: fetchWooVersion())

    /** The same check without the network fallback, for callers that must not fetch. */
    fun fromCachedVersion(): Boolean = supports(getWooVersion())

    private fun supports(wooVersion: String?) =
        wooVersion != null && wooVersion.semverCompareTo(MIN_WC_VERSION) >= 0

    companion object {
        const val MIN_WC_VERSION = "10.5.0"
    }
}
