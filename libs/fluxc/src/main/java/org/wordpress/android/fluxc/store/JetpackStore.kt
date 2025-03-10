package org.wordpress.android.fluxc.store

import com.android.volley.VolleyError
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpack.JetpackConnectionData
import org.wordpress.android.fluxc.network.rest.wpapi.jetpack.JetpackWPAPIRestClient
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

private const val JETPACK_DOMAIN = "jetpack.wordpress.com"

@Singleton
class JetpackStore @Inject constructor(
    private val jetpackWPAPIRestClient: JetpackWPAPIRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchJetpackConnectionUrl(
        site: SiteModel,
        useApplicationPasswords: Boolean,
        autoRegisterSiteIfNeeded: Boolean,
    ): JetpackResult<String> {
        if (site.isUsingWpComRestApi) error("This function supports only self-hosted site using WPAPI")
        return coroutineEngine.withDefaultContext(T.API, this, "fetchJetpackConnectionUrl") {
            val result = jetpackWPAPIRestClient.fetchJetpackConnectionUrl(site, useApplicationPasswords)

            result.toJetpackResult { result ->
                val url = result.trim('"').replace("\\", "")
                val connectionUri = URI.create(url)
                if (!autoRegisterSiteIfNeeded || connectionUri.host == JETPACK_DOMAIN || useApplicationPasswords) {
                    JetpackResult(url)
                } else {
                    jetpackWPAPIRestClient.registerJetpackSiteUsingCookies(url).fold(
                        onSuccess = {
                            JetpackResult(it)
                        },
                        onFailure = {
                            val errorCode = (it as? VolleyError)?.networkResponse?.statusCode
                            JetpackResult(
                                JetpackError(
                                    message = it.message,
                                    errorCode = errorCode
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    suspend fun fetchJetpackConnectionData(
        site: SiteModel,
        useApplicationPasswords: Boolean
    ): JetpackResult<JetpackConnectionData> {
        if (site.isUsingWpComRestApi) error("This function is not implemented yet for Jetpack tunnel")
        return coroutineEngine.withDefaultContext(T.API, this, "fetchJetpackConnectionData") {
            val result = jetpackWPAPIRestClient.fetchJetpackConnectionData(site, useApplicationPasswords)

            result.toJetpackResult { result ->
                JetpackResult(result)
            }
        }
    }

    /**
     * Register a site with Jetpack
     *
     * @return the blog ID of the registered site
     */
    suspend fun registerSite(
        site: SiteModel,
        useApplicationPasswords: Boolean
    ): JetpackResult<Long> {
        if (site.isUsingWpComRestApi) error("This function is not implemented yet for Jetpack tunnel")
        return coroutineEngine.withDefaultContext(T.API, this, "registerSite") {
            val result = jetpackWPAPIRestClient.registerSite(site, useApplicationPasswords)

            result.toJetpackResult { result ->
                JetpackResult(result)
            }
        }
    }

    suspend fun connectJetpackAccount(
        site: SiteModel,
        blogId: Long,
        useApplicationPasswords: Boolean
    ): JetpackResult<Unit> {
        return coroutineEngine.withDefaultContext(T.API, this, "connectJetpackAccount") {
            val provision = jetpackWPAPIRestClient.provisionConnection(site, useApplicationPasswords).also {
                if (it.isError) return@withDefaultContext JetpackResult<Unit>(
                    JetpackError(
                        it.error?.message,
                        it.error?.volleyError?.networkResponse?.statusCode
                    )
                )
            }

            val result = jetpackWPAPIRestClient.connectJetpackAccount(
                site = site,
                blogId = blogId,
                provisioningParams = provision.result!!
            )

            result.toJetpackResult { JetpackResult(Unit) }
        }
    }

    data class JetpackResult<T>(
        val data: T?
    ) : Payload<JetpackError>() {
        constructor(error: JetpackError) : this(null) {
            this.error = error
        }
    }

    data class JetpackError(
        val message: String? = null,
        val errorCode: Int? = null
    ) : OnChangedError

    private suspend inline fun <T, R> JetpackWPAPIRestClient.JetpackWPAPIPayload<T>.toJetpackResult(
        transform: suspend (T) -> JetpackResult<R>
    ): JetpackResult<R> {
        return when {
            isError -> JetpackResult(JetpackError(error?.message, error?.volleyError?.networkResponse?.statusCode))
            result == null -> JetpackResult(JetpackError("Response Empty"))
            else -> transform(result)
        }
    }
}
