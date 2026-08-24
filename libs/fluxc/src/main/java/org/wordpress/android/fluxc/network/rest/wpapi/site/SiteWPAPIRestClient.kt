package org.wordpress.android.fluxc.network.rest.wpapi.site

import com.android.volley.RequestQueue
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
import org.wordpress.android.fluxc.utils.HttpsUrlNormalizer
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SiteWPAPIRestClient @Inject constructor(
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    private val discoveryWPAPIRestClient: DiscoveryWPAPIRestClient,
    private val httpsUrlNormalizer: HttpsUrlNormalizer,
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
        val inputUrl = try {
            httpsUrlNormalizer.normalize(payload.url, addHttpsSchemeIfMissing = true)
        } catch (e: IllegalArgumentException) {
            return SiteModel().apply { error = BaseNetworkError(INVALID_RESPONSE) }
        }
        val cleanedUrl = DiscoveryUtils.stripKnownPaths(inputUrl.normalizedUrl)
        val discoveredWpApiUrl = try {
            httpsUrlNormalizer.normalize(discoverApiEndpoint(cleanedUrl)).normalizedUrl
        } catch (e: IllegalArgumentException) {
            return SiteModel().apply { error = BaseNetworkError(INVALID_RESPONSE) }
        }

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
                val serverUrl = try {
                    response.url?.let(httpsUrlNormalizer::normalize)
                } catch (e: IllegalArgumentException) {
                    return SiteModel().apply { error = BaseNetworkError(INVALID_RESPONSE) }
                }

                SiteModel().apply {
                    name = response.name
                    timezone = response.gmtOffset
                    origin = SiteModel.ORIGIN_WPAPI
                    httpsConfigurationState = configurationState(
                        serverUrl,
                        inputUrl.wasUpgraded || payload.wasUrlNormalizedToHttps,
                    )
                    hasWooCommerce = response.namespaces.any {
                        it.startsWith(WOO_API_NAMESPACE_PREFIX)
                    }

                    applicationPasswordsAuthorizeUrl = response.authentication?.applicationPasswords
                        ?.endpoints?.authorization
                        ?.normalizeOptionalUrl()
                    adminUrl = inferAdminBaseUrl(applicationPasswordsAuthorizeUrl)

                    wpApiRestUrl = discoveredWpApiUrl
                    this.url = serverUrl?.normalizedUrl ?: cleanedUrl
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
                wasUrlNormalizedToHttps = site.url.startsWith("http://", ignoreCase = true),
            )
        ).also { refreshedSite ->
            if (!refreshedSite.isError) {
                refreshedSite.id = site.id
                if (refreshedSite.httpsConfigurationState == SiteModel.HTTPS_CONFIGURATION_UNKNOWN) {
                    refreshedSite.httpsConfigurationState = site.httpsConfigurationState
                }
                site.loginUrl?.takeUnless(String::isBlank)?.normalizeOptionalUrl()
                    ?.let { refreshedSite.loginUrl = it }
                site.adminUrl
                    ?.takeUnless(String::isBlank)
                    ?.takeUnless { it.matchesAdminBaseInferredFrom(site.applicationPasswordsAuthorizeUrl) }
                    ?.normalizeOptionalUrl()
                    ?.let { refreshedSite.adminUrl = it }
            }
        }
    }

    @SiteModel.HttpsConfigurationState
    private fun configurationState(serverUrl: HttpsUrlNormalizer.Result?, inputWasUpgraded: Boolean): Int {
        return when {
            serverUrl?.wasUpgraded == true -> SiteModel.HTTPS_CONFIGURATION_REQUIRES_HTTPS
            serverUrl != null -> SiteModel.HTTPS_CONFIGURATION_SECURE
            inputWasUpgraded -> SiteModel.HTTPS_CONFIGURATION_REQUIRES_HTTPS
            else -> SiteModel.HTTPS_CONFIGURATION_UNKNOWN
        }
    }

    private fun String.normalizeOptionalUrl(): String? =
        runCatching { httpsUrlNormalizer.normalize(this).normalizedUrl }.getOrNull()

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
