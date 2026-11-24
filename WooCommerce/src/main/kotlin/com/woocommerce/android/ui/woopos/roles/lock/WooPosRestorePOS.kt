package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class WooPosRestorePOS @Inject constructor(
    private val preferences: WooPosLockPreferences,
    private val selectedSite: SelectedSite,
    private val featureFlags: FeatureFlagRepository,
    private val rateLimiter: WooPosLocalRateLimiter,
) {
    suspend operator fun invoke(): Boolean {
        if (!featureFlags.isEnabled(FeatureFlag.WOO_POS_ROLES)) {
            preferences.clearRestoreState()
            return false
        }
        val siteId = selectedSite.getOrNull()?.id ?: return false
        val logoutVersion = rateLimiter.logoutVersion.value
        val saved = preferences.restoreSiteId()
        if (saved.isSuccess && saved.getOrThrow() == null) return false
        fun isCurrent() = siteId == selectedSite.getOrNull()?.id && logoutVersion == rateLimiter.logoutVersion.value
        if (!isCurrent()) return false
        if (saved.isSuccess && saved.getOrThrow() != siteId) {
            preferences.clearRestoreState(saved.getOrThrow()) { isCurrent() }
            return false
        }
        val protection = rateLimiter.isProtectionEnabled(siteId)
        if (siteId != selectedSite.getOrNull()?.id || logoutVersion != rateLimiter.logoutVersion.value) return false
        if (!featureFlags.isEnabled(FeatureFlag.WOO_POS_ROLES)) return false
        if (protection.getOrNull() == false) {
            preferences.clearRestoreState(siteId) { isCurrent() }
            return false
        }
        return true
    }
}
