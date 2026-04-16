package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class WooPosIsWooCommerceVersionSunsetWarningRequired @Inject constructor(
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val dateTimeProvider: DateTimeProvider,
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(): Boolean {
        val wooCommerceVersion = getWooCoreVersion() ?: return false
        if (wooCommerceVersion.semverCompareTo(REQUIRED_WC_VERSION) >= 0) return false

        val dismissalTimestamp = preferencesRepository.getWooVersionSunsetBannerDismissalTimestamp()
        if (dismissalTimestamp != null) {
            val elapsed = dateTimeProvider.now() - dismissalTimestamp
            if (elapsed < COOLDOWN_DURATION_MS) return false
        }
        return true
    }

    companion object {
        const val REQUIRED_WC_VERSION = "10.5.0"
        val COOLDOWN_DURATION_MS = 14.days.inWholeMilliseconds
    }
}
