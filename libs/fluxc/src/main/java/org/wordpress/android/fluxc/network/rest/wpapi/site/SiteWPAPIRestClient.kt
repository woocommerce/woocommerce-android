package org.wordpress.android.fluxc.network.rest.wpapi.site

import com.android.volley.RequestQueue
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.DiscoveryUtils
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIDiscoveryUtils
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SiteWPAPIRestClient @Inject constructor(
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    dispatcher: Dispatcher,
    @Named("custom-ssl") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    companion object {
        private const val WOO_API_NAMESPACE_PREFIX = "wc/"
        private const val FETCH_API_CALL_FIELDS =
            "name,description,gmt_offset,url,authentication,namespaces"
        private const val APPLICATION_PASSWORDS_URL_SUFFIX = "authorize-application.php"
    }

    suspend fun fetchWPAPISite(
        payload: FetchWPAPISitePayload
    ): SiteModel {
        val cleanedUrl = UrlUtils.addUrlSchemeIfNeeded(payload.url, false).let { urlWithScheme ->
            DiscoveryUtils.stripKnownPaths(urlWithScheme)
        }

        val discoveredWpApiUrl = discoverApiEndpoint(cleanedUrl)
        val urlScheme = discoveredWpApiUrl.toHttpUrl().scheme

        val result = wpapiGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = discoveredWpApiUrl,
            clazz = RootWPAPIRestResponse::class.java,
            params = mapOf("_fields" to FETCH_API_CALL_FIELDS)
        )

        return when (result) {
            is Success -> {
                val response = result.data
                if (response?.namespaces.isNullOrEmpty()) {
                    return SiteModel().apply {
                        error = BaseNetworkError(INVALID_RESPONSE)
                    }
                }

                SiteModel().apply {
                    name = response.name
                    timezone = response.gmtOffset
                    origin = SiteModel.ORIGIN_WPAPI
                    hasWooCommerce = response.namespaces.any {
                        it.startsWith(WOO_API_NAMESPACE_PREFIX)
                    }

                    applicationPasswordsAuthorizeUrl = response.authentication?.applicationPasswords
                        ?.endpoints?.authorization
                    adminUrl = inferAdminBaseUrl(applicationPasswordsAuthorizeUrl)

                    wpApiRestUrl = discoveredWpApiUrl
                    this.url = cleanedUrl.replaceBefore("://", urlScheme)
                    this.username = payload.username
                    this.password = payload.password
                }
            }

            is Error -> {
                SiteModel().apply {
                    error = result.error
                }
            }
        }
    }

    suspend fun fetchWPAPISite(
        site: SiteModel
    ): SiteModel {
        return fetchWPAPISite(
            payload = FetchWPAPISitePayload(
                url = site.url,
                username = site.username,
                password = site.password,
            )
        ).also { refreshedSite ->
            if (!refreshedSite.isError) {
                site.loginUrl?.takeUnless(String::isBlank)?.let { refreshedSite.loginUrl = it }
                site.adminUrl
                    ?.takeUnless(String::isBlank)
                    ?.takeUnless { it.matchesAdminBaseInferredFrom(site.applicationPasswordsAuthorizeUrl) }
                    ?.let { refreshedSite.adminUrl = it }
            }
        }
    }

    private fun inferAdminBaseUrl(applicationPasswordsAuthorizeUrl: String?): String? =
        applicationPasswordsAuthorizeUrl
            ?.takeUnless(String::isBlank)
            ?.takeIf { it.contains(APPLICATION_PASSWORDS_URL_SUFFIX) }
            ?.substringBefore(APPLICATION_PASSWORDS_URL_SUFFIX)

    private fun String.matchesAdminBaseInferredFrom(applicationPasswordsAuthorizeUrl: String?): Boolean {
        val inferredAdminBase = inferAdminBaseUrl(applicationPasswordsAuthorizeUrl) ?: return false
        return normalizeAdminBase(this) == normalizeAdminBase(inferredAdminBase)
    }

    private fun normalizeAdminBase(url: String) = url.trim().trimEnd('/')

    private fun discoverApiEndpoint(
        url: String
    ): String {
        return discoveryWPAPIRestClient.discoverWPAPIBaseURL(url) // discover rest api endpoint
            ?: WPAPIDiscoveryUtils.buildDefaultRESTBaseUrl(url)
    }
}
