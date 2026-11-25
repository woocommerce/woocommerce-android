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
        featureFlags.awaitRemoteFlagsLoaded()
        if (!featureFlags.isEnabled(FeatureFlag.WOO_POS_ROLES)) {
            preferences.clearRestoreState()
            return false
        }
        val siteId = selectedSite.getOrNull()?.id ?: return false
        val logoutVersion = rateLimiter.logoutVersion.value
        fun isCurrent() = siteId == selectedSite.getOrNull()?.id && logoutVersion == rateLimiter.logoutVersion.value
        val saved = preferences.restoreSiteId()
        return when {
            saved.isSuccess && saved.getOrThrow() == null -> false
            !isCurrent() -> false
            saved.isSuccess && saved.getOrThrow() != siteId -> {
                preferences.clearRestoreState(saved.getOrThrow()) { isCurrent() }
                false
            }
            else -> shouldRestoreProtectedSite(siteId, ::isCurrent)
        }
    }

    private suspend fun shouldRestoreProtectedSite(siteId: Int, isCurrent: () -> Boolean): Boolean {
        val protection = rateLimiter.isProtectionEnabled(siteId)
        return when {
            !isCurrent() -> false
            !featureFlags.isEnabled(FeatureFlag.WOO_POS_ROLES) -> {
                preferences.clearRestoreState(siteId, isCurrent)
                false
            }
            protection.getOrNull() == false -> {
                preferences.clearRestoreState(siteId, isCurrent)
                false
            }
            else -> true
        }
    }
}
