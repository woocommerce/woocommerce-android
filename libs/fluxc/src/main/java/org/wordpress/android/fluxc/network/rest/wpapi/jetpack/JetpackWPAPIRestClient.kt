package org.wordpress.android.fluxc.network.rest.wpapi.jetpack

import com.android.volley.Request
import com.android.volley.RequestQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.JPAPI
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpack.JetpackConnectionData
import org.wordpress.android.fluxc.model.jetpack.JetpackUser
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.RawRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class JetpackWPAPIRestClient @Inject constructor(
    private val wpApiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    private val cookieNonceAuthenticator: CookieNonceAuthenticator,
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork,
    private val wpComNetwork: WPComNetwork,
    dispatcher: Dispatcher,
    @Named("custom-ssl") requestQueue: RequestQueue,
    @Named("no-redirects") private val noRedirectsRequestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    suspend fun fetchJetpackConnectionUrl(
        site: SiteModel,
        useApplicationPasswords: Boolean = false
    ): JetpackWPAPIPayload<String> {
        val response = makeGetWPAPIRequest<String>(
            site = site,
            path = JPAPI.connection.url.pathV4,
            useApplicationPasswords = useApplicationPasswords
        )

        return when (response) {
            is Success<String> -> JetpackWPAPIPayload(response.data)
            is Error -> JetpackWPAPIPayload(response.error)
        }
    }

    suspend fun registerJetpackSiteUsingCookies(registrationUrl: String): Result<String> {
        @Suppress("MagicNumber")
        fun Int.isRedirect(): Boolean = this in 300..399
        return suspendCancellableCoroutine { cont ->
            val request = RawRequest(
                method = Request.Method.GET,
                url = registrationUrl,
                listener = {
                    cont.resume(Result.failure(Exception("Got a success response instead of the expected redirect")))
                },
                onErrorListener = { error ->
                    val response = error.volleyError.networkResponse

                    if (response == null || !response.statusCode.isRedirect()) {
                        cont.resume(Result.failure(error.volleyError))
                        return@RawRequest
                    }

                    response.headers?.get("Location")?.let {
                        if (it.isNotEmpty()) {
                            cont.resume(Result.success(it))
                        } else {
                            cont.resume(Result.failure(Exception("Location header missing")))
                        }
                    }
                }
            )

            noRedirectsRequestQueue.add(request)

            cont.invokeOnCancellation {
                request.cancel()
            }
        }
    }

    suspend fun fetchJetpackConnectionData(
        site: SiteModel,
        useApplicationPasswords: Boolean = false
    ): JetpackWPAPIPayload<JetpackConnectionData> {
        val response = makeGetWPAPIRequest<JetpackConnectionDataResponse>(
            site = site,
            path = JPAPI.connection.data.pathV4,
            useApplicationPasswords = useApplicationPasswords
        )

        return when (response) {
            is Success<JetpackConnectionDataResponse> -> JetpackWPAPIPayload(
                    response.data?.toDomainModel()
            )

            is Error -> JetpackWPAPIPayload(response.error)
        }
    }

    /**
     * Establishes a site-level connection between the site and WordPress.com using Jetpack.
     *
     * @return a [JetpackWPAPIPayload] with the blog_id of the site
     */
    suspend fun registerSite(
        site: SiteModel,
        useApplicationPasswords: Boolean
    ): JetpackWPAPIPayload<Long> {
        val response = makePostWPAPIRequest<JetpackConnectionRegisterResponse>(
            site = site,
            path = JPAPI.connection.register.pathV4,
            body = mapOf(
                "from" to "woocommerce_android",
                "plugin_slug" to "jetpack"
            ),
            useApplicationPasswords = useApplicationPasswords
        )

        return when (response) {
            is Success<JetpackConnectionRegisterResponse> -> {
                val blogId =
                    response.data?.authorizeUrl?.toHttpUrl()?.queryParameter("client_id")?.toLong()

                if (blogId == null) {
                    JetpackWPAPIPayload(
                        BaseNetworkError(
                            BaseRequest.GenericErrorType.INVALID_RESPONSE,
                            "Invalid blog_id"
                        )
                    )
                } else {
                    JetpackWPAPIPayload(blogId)
                }
            }

            is Error -> JetpackWPAPIPayload(response.error)
        }
    }

    suspend fun provisionConnection(
        site: SiteModel,
        useApplicationPasswords: Boolean
    ): JetpackWPAPIPayload<JetpackConnectionProvisionResponse> {
        val response = makePostWPAPIRequest<JetpackConnectionProvisionResponse>(
            site = site,
            path = JPAPI.remote_provision.pathV4,
            body = emptyMap(),
            useApplicationPasswords = useApplicationPasswords
        )

        return when (response) {
            is Success<JetpackConnectionProvisionResponse> -> {
                JetpackWPAPIPayload(response.data)
            }

            is Error -> JetpackWPAPIPayload(response.error)
        }
    }

    suspend fun connectJetpackAccount(
        site: SiteModel,
        blogId: Long,
        provisioningParams: JetpackConnectionProvisionResponse
    ): JetpackWPAPIPayload<Unit> {
        val response = wpComNetwork.executePostGsonRequest(
            url = WPCOMV2.sites.site(blogId).jetpack_remote_connect_user.url,
            body = mapOf(
                "redirect_uri" to site.url,
                "secret" to provisioningParams.secret,
                "scope" to provisioningParams.scope,
                "external_user_id" to provisioningParams.userId.toString()
            ),
            clazz = Unit::class.java
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success<Unit> -> JetpackWPAPIPayload(Unit)
            is WPComGsonRequestBuilder.Response.Error -> JetpackWPAPIPayload(response.error)
        }
    }

    private suspend inline fun <reified T> makeGetWPAPIRequest(
        site: SiteModel,
        path: String,
        useApplicationPasswords: Boolean
    ): WPAPIResponse<T> {
        return if (useApplicationPasswords) {
            applicationPasswordsNetwork.executeGetGsonRequest(
                site = site,
                path = path,
                clazz = T::class.java
            )
        } else {
            val url = site.buildUrl(path)
            cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
                wpApiGsonRequestBuilder.syncGetRequest(
                    restClient = this,
                    url = url,
                    nonce = nonce.value,
                    clazz = T::class.java
                )
            }
        }
    }

    private suspend inline fun <reified T> makePostWPAPIRequest(
        site: SiteModel,
        path: String,
        body: Map<String, String>,
        useApplicationPasswords: Boolean
    ): WPAPIResponse<T> {
        return if (useApplicationPasswords) {
            applicationPasswordsNetwork.executePostGsonRequest(
                site = site,
                path = path,
                body = body,
                clazz = T::class.java
            )
        } else {
            val url = site.buildUrl(path)
            cookieNonceAuthenticator.makeAuthenticatedWPAPIRequest(site) { nonce ->
                wpApiGsonRequestBuilder.syncPostRequest(
                    restClient = this,
                    url = url,
                    nonce = nonce.value,
                    body = body,
                    clazz = T::class.java
                )
            }
        }
    }

    private fun JetpackConnectionDataResponse.toDomainModel(): JetpackConnectionData {
        return JetpackConnectionData(
            isSiteRegistered = isRegistered,
            blogId = currentUser.blogId?.let { if (it.isNumber) it.asLong else null },
            currentUser = JetpackUser(
                isConnected = currentUser.isConnected ?: false,
                isMaster = currentUser.isMaster ?: false,
                username = currentUser.username.orEmpty(),
                wpcomEmail = currentUser.wpcomUser?.email.orEmpty(),
                wpcomId = currentUser.wpcomUser?.id ?: 0L,
                wpcomUsername = currentUser.wpcomUser?.login.orEmpty()
            ),
            connectionOwner = connectionOwner
        )
    }

    private fun SiteModel.buildUrl(path: String): String {
        val baseUrl = wpApiRestUrl ?: "${url}/wp-json"
        return "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
    }

    data class JetpackWPAPIPayload<T>(
        val result: T?
    ) : Payload<BaseNetworkError?>() {
        constructor(error: BaseNetworkError) : this(null) {
            this.error = error
        }
    }
}
