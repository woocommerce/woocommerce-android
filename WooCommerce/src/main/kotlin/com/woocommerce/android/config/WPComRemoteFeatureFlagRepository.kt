package com.woocommerce.android.config

import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WPComRemoteFeatureFlagRepository @Inject constructor(
    private val featureFlagsStore: FeatureFlagsStore
) {
    companion object {
        private const val PLATFORM_NAME = "android"
    }
    // Fetch all WPCOM feature flags for Android, and optionally only for certain app version
    suspend fun fetchFeatureFlags(appVersion: String = "") =
        featureFlagsStore.fetchFeatureFlags(
        buildNumber = "",
        deviceId = "",
        identifier = "",
        marketingVersion = appVersion,
        platform = PLATFORM_NAME
    )
}
