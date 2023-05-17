package com.woocommerce.android.config

import com.woocommerce.android.OnChangedException
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WPComRemoteFeatureFlagRepository @Inject constructor(
    private val featureFlagsStore: FeatureFlagsStore
) {
    companion object {
        private const val PLATFORM_NAME = "android"
    }

    suspend fun fetchFeatureFlags(appVersion: String = ""): Result<Unit> {
        // Empty string are parameters not used by this app.
        val result = featureFlagsStore.fetchFeatureFlags(
            buildNumber = "",
            deviceId = "",
            identifier = "",
            marketingVersion = appVersion,
            platform = PLATFORM_NAME
        )
        return if (result.isError) {
            AppLog.e(AppLog.T.API, "Error fetching WPCom remote feature flags: ${result.error}")

            Result.failure(OnChangedException(result.error, result.error.message))
        } else {
            AppLog.i(AppLog.T.API, "Successfully fetched WPCom remote feature flags")
            Result.success(Unit)
        }
    }

    fun isRemoteFeatureFlagEnabled(key: String): Boolean =
        featureFlagsStore.getFeatureFlagsByKey(key).firstOrNull()?.value ?: false
}
