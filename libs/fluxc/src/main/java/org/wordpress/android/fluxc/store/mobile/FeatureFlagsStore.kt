package org.wordpress.android.fluxc.store.mobile

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsError
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsRestClient.FeatureFlagsPayload
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlag
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagsStore @Inject constructor(
    private val featureFlagsRestClient: FeatureFlagsRestClient,
    private val featureFlagConfigDao: FeatureFlagConfigDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchFeatureFlags(
        buildNumber: String,
        deviceId: String,
        identifier: String,
        marketingVersion: String,
        platform: String,
        localSiteId: LocalId = DEFAULT_LOCAL_SITE_ID,
        activePluginVersions: Map<String, String> = emptyMap()
    ) = fetchFeatureFlags(
        payload = FeatureFlagsPayload(
            buildNumber = buildNumber,
            deviceId = deviceId,
            identifier = identifier,
            marketingVersion = marketingVersion,
            platform = platform,
            activePluginVersions = activePluginVersions
        ),
        localSiteId = localSiteId
    )

    suspend fun fetchFeatureFlags(
        payload: FeatureFlagsPayload,
        localSiteId: LocalId = DEFAULT_LOCAL_SITE_ID
    ) =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetch feature-flags") {
            val payload = featureFlagsRestClient.fetchFeatureFlags(payload)
            return@withDefaultContext when {
                payload.isError -> FeatureFlagsResult(payload.error)
                payload.featureFlags != null -> {
                    featureFlagConfigDao.insert(payload.featureFlags, localSiteId)
                    FeatureFlagsResult(payload.featureFlags)
                }

                else -> FeatureFlagsResult(FeatureFlagsError(GENERIC_ERROR))
            }
        }

    fun observeFeatureFlags(localSiteId: LocalId = DEFAULT_LOCAL_SITE_ID): Flow<List<FeatureFlag>> {
        return featureFlagConfigDao.observeFeatureFlagList(localSiteId)
    }

    data class FeatureFlagsResult(
        val featureFlags: Map<String, Boolean>? = null
    ) : Store.OnChanged<FeatureFlagsError>() {
        constructor(error: FeatureFlagsError) : this() {
            this.error = error
        }
    }

    companion object {
        val DEFAULT_LOCAL_SITE_ID = LocalId(-1)
    }
}
