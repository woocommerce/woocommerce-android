package org.wordpress.android.fluxc.network.rest.wpcom.mobile

import android.content.Context
import android.os.Build
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FeatureFlagsRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchFeatureFlags(payload: FeatureFlagsPayload): FeatureFlagsFetchedPayload {
        val url = WPCOMV2.mobile.feature_flags.url
        val params = buildFeatureFlagsParams(payload)
        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            params,
            Map::class.java
        )
        return when (response) {
            is Response.Success -> buildFeatureFlagsFetchedPayload(response.data)
            is Response.Error -> FeatureFlagsFetchedPayload(response.error.toFeatureFlagsError())
        }
    }

    private fun buildFeatureFlagsParams(payload: FeatureFlagsPayload) = buildMap {
        put("build_number", payload.buildNumber)
        put("device_id", payload.deviceId)
        put("identifier", payload.identifier)
        put("marketing_version", payload.marketingVersion)
        put("platform", payload.platform)
        put("os_version", payload.osVersion)
        payload.activePluginVersions.forEach { (pluginPath, version) ->
            put("active_plugin_versions[$pluginPath]", version)
        }
    }

    data class FeatureFlagsPayload(
        val buildNumber: String,
        val deviceId: String,
        val identifier: String,
        val marketingVersion: String,
        val platform: String,
        val osVersion: String = Build.VERSION.RELEASE,
        val activePluginVersions: Map<String, String> = emptyMap(),
    )

    private fun buildFeatureFlagsFetchedPayload(featureFlags: Map<*, *>?): FeatureFlagsFetchedPayload {
        return FeatureFlagsFetchedPayload(featureFlags?.map { e ->
                e.key.toString() to e.value.toString().toBoolean()
            }?.toMap())
    }
}

data class FeatureFlagsFetchedPayload(
    val featureFlags: Map<String, Boolean>? = null
) : Payload<FeatureFlagsError>() {
    constructor(error: FeatureFlagsError) : this() {
        this.error = error
    }
}

class FeatureFlagsError(
    val type: FeatureFlagsErrorType,
    val message: String? = null
) : OnChangedError

enum class FeatureFlagsErrorType {
    API_ERROR,
    AUTH_ERROR,
    GENERIC_ERROR,
    INVALID_RESPONSE,
    TIMEOUT,
}

fun WPComGsonNetworkError.toFeatureFlagsError(): FeatureFlagsError {
    val type = when (type) {
        GenericErrorType.TIMEOUT -> FeatureFlagsErrorType.TIMEOUT
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.SERVER_ERROR,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR -> FeatureFlagsErrorType.API_ERROR
        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE -> FeatureFlagsErrorType.INVALID_RESPONSE
        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED -> FeatureFlagsErrorType.AUTH_ERROR
        GenericErrorType.UNKNOWN -> GENERIC_ERROR
        null -> GENERIC_ERROR
    }
    return FeatureFlagsError(type, message)
}
