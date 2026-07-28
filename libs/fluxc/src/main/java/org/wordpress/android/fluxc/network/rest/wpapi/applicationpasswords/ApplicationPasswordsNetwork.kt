package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Credentials
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.HttpMethod
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.toVolleyMethod
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.AppLog
import java.util.Optional
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val AUTHORIZATION_HEADER = "Authorization"
private const val UNAUTHORIZED = 401

@Singleton
class ApplicationPasswordsNetwork @Inject constructor(
    @Named("no-cookies") private val requestQueue: RequestQueue,
    private val userAgent: UserAgent,
    private val listener: Optional<ApplicationPasswordsListener>
) : WPAPINetwork {
    // We can't use construction injection for this variable, as its class is internal
    @Inject
    internal lateinit var mApplicationPasswordsManager: ApplicationPasswordsManager

    @Suppress("ComplexMethod")
    private suspend fun <T> executeGsonRequest(
        site: SiteModel,
        method: HttpMethod,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap(),
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        forced: Boolean = false,
        requestTimeout: Int = BaseRequest.DEFAULT_REQUEST_TIMEOUT,
        retries: Int = BaseRequest.DEFAULT_MAX_RETRIES,
        isRegeneratingApplicationPassword: Boolean = false
    ): WPAPIResponse<T> {
        fun buildRequest(
            continuation: Continuation<WPAPIResponse<T>>,
            authorizationHeader: String
        ): WPAPIGsonRequest<T> {
            val request = WPAPIGsonRequest(
                method.toVolleyMethod(),
                (site.wpApiRestUrl ?: site.url.slashJoin("wp-json")).slashJoin(path),
                params,
                body,
                clazz,
                /* listener = */
                { responseData, headers -> continuation.resume(WPAPIResponse.Success(responseData, headers)) },
                /* errorListener = */
                { continuation.resume(WPAPIResponse.Error(it)) }
            )

            request.addHeader(AUTHORIZATION_HEADER, authorizationHeader)
            request.setUserAgent(userAgent.apiUserAgent)

            if (enableCaching) {
                request.enableCaching(cacheTimeToLive)
            }
            if (forced) {
                request.setShouldForceUpdate()
            }

            request.retryPolicy = DefaultRetryPolicy(
                requestTimeout,
                retries,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )

            return request
        }

        val credentialsResult = mApplicationPasswordsManager.getApplicationCredentials(site)
        val credentials = when (credentialsResult) {
            is ApplicationPasswordCreationResult.Existing -> credentialsResult.credentials
            is ApplicationPasswordCreationResult.Created -> {
                if (listener.isPresent) {
                    listener.get().onNewPasswordCreated(isRegeneratingApplicationPassword)
                }
                credentialsResult.credentials
            }

            is ApplicationPasswordCreationResult.Failure -> {
                if (listener.isPresent) {
                    listener.get().onPasswordGenerationFailed(credentialsResult.error.toWPAPINetworkError())
                }
                return WPAPIResponse.Error(credentialsResult.error.toWPAPINetworkError().asGenerationFailure())
            }

            is ApplicationPasswordCreationResult.NotSupported -> {
                val networkError = credentialsResult.originalError.toWPAPINetworkError()
                if (listener.isPresent) {
                    listener.get().onFeatureUnavailable(site, networkError)
                }
                return WPAPIResponse.Error(networkError.asGenerationFailure())
            }
        }

        val authorizationHeader = Credentials.basic(credentials.userName, credentials.password)

        val response = suspendCancellableCoroutine { continuation ->
            val request = buildRequest(continuation, authorizationHeader)
            requestQueue.add(request)

            continuation.invokeOnCancellation {
                request.cancel()
            }
        }

        return if (credentialsResult is ApplicationPasswordCreationResult.Existing &&
            response is WPAPIResponse.Error &&
            response.error.volleyError?.networkResponse?.statusCode == UNAUTHORIZED
        ) {
            AppLog.w(
                AppLog.T.MAIN,
                "Authentication failure using application password, maybe revoked?" +
                    " Delete the saved one then retry"
            )
            mApplicationPasswordsManager.deleteLocalApplicationPassword(site, credentials)
            executeGsonRequest(site, method, path, clazz, params, body, isRegeneratingApplicationPassword = true)
        } else {
            response
        }
    }

    override suspend fun <T : Any> executeGetGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String>,
        enableCaching: Boolean,
        cacheTimeToLive: Int,
        forced: Boolean,
        requestTimeout: Int,
        retries: Int
    ) = executeGsonRequest(
        site = site,
        method = HttpMethod.GET,
        path = path,
        clazz = clazz,
        params = params,
        enableCaching = enableCaching,
        cacheTimeToLive = cacheTimeToLive,
        forced = forced,
        requestTimeout = requestTimeout,
        retries = retries
    )

    override suspend fun <T : Any> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>
    ) = executeGsonRequest(site, HttpMethod.POST, path, clazz, body = body)

    override suspend fun <T : Any> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any>,
        params: Map<String, String>
    ) = executeGsonRequest(site, HttpMethod.PUT, path, clazz, params = params, body = body)

    override suspend fun <T : Any> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String>
    ) = executeGsonRequest(site, HttpMethod.DELETE, path, clazz, params)

    private fun WPAPINetworkError.asGenerationFailure(): WPAPINetworkError {
        val newErrorCode = "$APP_PASSWORDS_GENERATION_FAILURE_ERROR_CODE_PREFIX${errorCode.orEmpty()}"
        return WPAPINetworkError(this, newErrorCode)
    }

    companion object {
        /**
         * A prefix that we use to allow differentiating errors caused by app passwords generation
         * from other regular API errors.
         */
        const val APP_PASSWORDS_GENERATION_FAILURE_ERROR_CODE_PREFIX = "app_passwords_generation_failure:"
    }
}

private fun BaseNetworkError.toWPAPINetworkError(): WPAPINetworkError {
    return when (this) {
        is WPAPINetworkError -> this
        is WPComGsonNetworkError -> WPAPINetworkError(
            baseError = this,
            errorCode = this.apiError
        )

        else -> WPAPINetworkError(this)
    }
}
