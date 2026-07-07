package org.wordpress.android.fluxc.network.rest.wpapi.media

import com.google.gson.Gson
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.annotations.endpoint.WPAPIEndpoint
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordCreationResult
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsManager
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ApplicationPasswordsMediaRestClient @Inject constructor(
    @Named("no-cookies") okHttpClient: OkHttpClient,
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork,
    gson: Gson
) : BaseWPV2MediaRestClient(okHttpClient, gson) {
    @Inject
    internal lateinit var applicationPasswordsManager: ApplicationPasswordsManager

    override fun WPAPIEndpoint.getFullUrl(site: SiteModel): String {
        return (site.wpApiRestUrl ?: site.url.slashJoin("wp-json")).slashJoin(urlV2)
    }

    override suspend fun getAuthorizationHeader(site: SiteModel): AuthorizationHeaderResult {
        return when (val result = applicationPasswordsManager.getApplicationCredentials(site)) {
            is ApplicationPasswordCreationResult.Created -> AuthorizationHeaderResult.Success(
                Credentials.basic(result.credentials.userName, result.credentials.password)
            )

            is ApplicationPasswordCreationResult.Existing -> AuthorizationHeaderResult.Success(
                Credentials.basic(result.credentials.userName, result.credentials.password)
            )

            is ApplicationPasswordCreationResult.Failure ->
                AuthorizationHeaderResult.Failure(result.error.toGenerationFailureMediaError())

            is ApplicationPasswordCreationResult.NotSupported ->
                AuthorizationHeaderResult.Failure(result.originalError.toGenerationFailureMediaError())
        }
    }

    override suspend fun <T : Any> executeGetGsonRequest(
        site: SiteModel,
        endpoint: WPAPIEndpoint,
        params: Map<String, String>,
        clazz: Class<T>
    ): WPAPIResponse<T> {
        return applicationPasswordsNetwork.executeGetGsonRequest(
            site = site,
            path = endpoint.urlV2,
            clazz = clazz,
            params = params
        )
    }

    private fun BaseNetworkError.toGenerationFailureMediaError(): MediaError {
        val originalErrorCode = when (this) {
            is WPAPINetworkError -> errorCode
            is WPComGsonNetworkError -> apiError
            else -> null
        }

        return MediaError(MediaErrorType.fromBaseNetworkError(this)).apply {
            statusCode = volleyError?.networkResponse?.statusCode ?: 0
            apiErrorCode = ApplicationPasswordsNetwork.APP_PASSWORDS_GENERATION_FAILURE_ERROR_CODE_PREFIX +
                originalErrorCode.orEmpty()
            message = this@toGenerationFailureMediaError.message
            logMessage = getCombinedErrorMessage()
        }
    }
}
