package com.woocommerce.android.config

import android.os.Build
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsRestClient
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

    /**
     * Fetches the latest state of all remote feature flags from backend. Behind-the-scene also automatically
     * saves data to DB.
     * @param appVersion (Optional) In the backend, a remote feature flag can be set with various rules based on
     * app version. This parameter can be used to work with those rules.
     */
    suspend fun fetchFeatureFlags(appVersion: String = ""): Result<Unit> {
        // Empty string are parameters not used by this app.
        val result = featureFlagsStore.fetchFeatureFlags(
            FeatureFlagsRestClient.FeatureFlagsPayload(
                buildNumber = "",
                deviceId = "",
                identifier = "",
                marketingVersion = appVersion,
                platform = PLATFORM_NAME,
                osVersion = Build.VERSION.RELEASE,
            )
        )
        return if (result.isError) {
            WooLog.e(WooLog.T.UTILS, "Error fetching WPCom remote feature flags: ${result.error}")

            Result.failure(OnChangedException(result.error, result.error.message))
        } else {
            WooLog.i(WooLog.T.UTILS, "Successfully fetched WPCom remote feature flags")
            Result.success(Unit)
        }
    }

    suspend fun isRemoteFeatureFlagEnabled(key: String): Boolean = withContext(Dispatchers.IO) {
        featureFlagsStore.getFeatureFlagsByKey(key).firstOrNull()?.value ?: false
    }
}
