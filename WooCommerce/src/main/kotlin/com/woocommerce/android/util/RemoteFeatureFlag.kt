package com.woocommerce.android.util

import com.woocommerce.android.config.WPComRemoteFeatureFlagRepository

enum class RemoteFeatureFlag(private val remoteKey: String) {
    LOCAL_NOTIFICATION_STORE_CREATION_READY("woo_notification_store_creation_ready"),
    LOCAL_NOTIFICATION_NUDGE_FREE_TRIAL_AFTER_1D("woo_notification_nudge_free_trial_after_1d"),
    LOCAL_NOTIFICATION_1D_BEFORE_FREE_TRIAL_EXPIRES("woo_notification_1d_before_free_trial_expires"),
    LOCAL_NOTIFICATION_1D_AFTER_FREE_TRIAL_EXPIRES("woo_notification_1d_after_free_trial_expires");

    companion object {
        lateinit var wpComRemoteFeatureFlagRepository: WPComRemoteFeatureFlagRepository
    }

    // Similar to how it works in `FeatureFlag`, add `PackageUtils.isDebugBuild()` to force-enable
    // the feature flag in debug builds.
    fun isEnabled(): Boolean {
        return when (this) {
            LOCAL_NOTIFICATION_STORE_CREATION_READY,
            LOCAL_NOTIFICATION_NUDGE_FREE_TRIAL_AFTER_1D,
            LOCAL_NOTIFICATION_1D_BEFORE_FREE_TRIAL_EXPIRES,
            LOCAL_NOTIFICATION_1D_AFTER_FREE_TRIAL_EXPIRES ->
                PackageUtils.isDebugBuild() || getRemoteFlagValue(remoteKey)
        }
    }

    private fun getRemoteFlagValue(key: String): Boolean {
        return wpComRemoteFeatureFlagRepository.getFeatureFlagByKey(key)?.value ?: false
    }
}
