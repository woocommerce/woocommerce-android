package org.wordpress.android.fluxc.network.rest.wpcom.site

import android.content.Context
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainError
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainErrorType
import org.wordpress.android.fluxc.store.SiteStore.DesignatedPrimaryDomainPayload
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesError
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesErrorType
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesResponsePayload
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.INVALID_SITE
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.REMOTE_SITE_CERTIFICATE_ERROR
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.TLS_CERTIFICATE_VALIDITY_ERROR
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainError
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainErrorType.EMPTY_RESULTS
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsResponsePayload
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import java.net.URI
import java.net.UnknownHostException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private val TLS_CERTIFICATE_VALIDITY_ERROR_TYPES = setOf(
    GenericErrorType.INVALID_SSL_CERTIFICATE,
    GenericErrorType.NO_CONNECTION,
    GenericErrorType.NETWORK_ERROR
)

@Suppress("LargeClass", "TooManyFunctions", "LongParameterList")
@Singleton
class SiteRestClient @Inject constructor(
    appContext: Context?,
    dispatcher: Dispatcher?,
    @Named("regular") requestQueue: RequestQueue?,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    private val coroutineEngine: CoroutineEngine,
    accessToken: AccessToken?,
    userAgent: UserAgent?
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     *  Fetches the user's sites from WPCom.
     *  Since the V1.2 endpoint doesn't return the plan features, we will handle the fetch by following two
     *  different approaches:
     *  1. If we don't need any filtering, then we'll simply use the v1.1 endpoint which includes the features.
     *  2. If we have some filters, then we'll send two requests: the first one to the v1.2 endpoint to fetch sites
     *     And the second one to the /me/sites/features to fetch the features separately, the combine the results.
     */
    @Suppress("ComplexMethod")
    suspend fun fetchSites(filters: List<SiteFilter?>, filterJetpackConnectedPackageSite: Boolean): SitesModel {
        val useV2Endpoint = filters.isNotEmpty()
        val params = getFetchSitesParams(filters)
        val url = WPCOMREST.me.sites.let { if (useV2Endpoint) it.urlV1_2 else it.urlV1_1 }
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, SitesResponse::class.java)

        val siteFeatures = if (useV2Endpoint) {
            fetchSitesFeatures().let {
                if (it is Error) {
                    val result = SitesModel()
                    result.error = it.error
                    return result
                }
                (it as Success).data
            }
        } else null

        return when (response) {
            is Success -> {
                val siteArray = mutableListOf<SiteModel>()
                val jetpackCPSiteArray = mutableListOf<SiteModel>()
                for (siteResponse in response.data.sites) {
                    val siteModel = siteResponseToSiteModel(siteResponse)

                    siteFeatures?.get(siteModel.siteId)?.let {
                        siteModel.planActiveFeatures = it.joinToString(",")
                    }

                    if (siteModel.isJetpackCPConnected) jetpackCPSiteArray.add(siteModel)
                    // see https://github.com/wordpress-mobile/WordPress-Android/issues/15540#issuecomment-993752880
                    if (filterJetpackConnectedPackageSite && siteModel.isJetpackCPConnected) continue
                    siteArray.add(siteModel)
                }

                val updatedJetpackCPSites = fetchAdditionalDetailsForJetpackCPSites(jetpackCPSiteArray)

                SitesModel(siteArray, updatedJetpackCPSites)
            }

            is Error -> {
                val payload = SitesModel(emptyList())
                payload.error = response.error
                payload
            }
        }
    }

    private suspend fun fetchAdditionalDetailsForJetpackCPSites(sites: List<SiteModel>): List<SiteModel> {
        return coroutineScope {
            sites.map { site ->
                async {
                    // Fetching the root endpoint will update the hasWooCommerce field and other site metadata
                    fetchSiteUsingRootEndpoint(site).let {
                        if (it.isError) {
                            // If there was an error just ignore it and return the original site
                            AppLog.w(
                                AppLog.T.API,
                                "Error fetching root endpoint for Jetpack CP connected site: ${site.url}, error: ${it.error}"
                            )
                            site
                        } else {
                            it
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun fetchSitesFeatures(): Response<Map<Long, List<String>>> {
        val url = WPCOMREST.me.sites.features.urlV1_1
        return wpComGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            params = emptyMap(),
            clazz = SitesFeaturesRestResponse::class.java
        ).let {
            when (it) {
                is Success -> Success(it.data.features.mapValues { it.value.active }, it.headers)
                is Error -> Error(it.error)
            }
        }
    }

    private fun getFetchSitesParams(filters: List<SiteFilter?>): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (filters.isNotEmpty()) params[FILTERS] = TextUtils.join(",", filters)
        params[FIELDS] = SITE_FIELDS
        return params
    }

    suspend fun fetchSite(site: SiteModel): SiteModel {
        return if (site.isJetpackConnected) {
            fetchSiteUsingWPComEndpoint(site).apply {
                fetchAppPasswordsAuthorizationUrl(site).onSuccess {
                    applicationPasswordsAuthorizeUrl = it
                }
            }
        } else {
            fetchSiteUsingRootEndpoint(site)
        }
    }

    private suspend fun fetchSiteUsingWPComEndpoint(site: SiteModel): SiteModel {
        val params = mutableMapOf<String, String>()
        params[FIELDS] = SITE_FIELDS
        val url = WPCOMREST.sites.urlV1_1 + site.siteId
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, SiteWPComRestResponse::class.java)
        return when (response) {
            is Success -> {
                val newSite = siteResponseToSiteModel(response.data)
                // local ID is not copied into the new model, let's make sure it is
                // otherwise the call that updates the DB can add a new row?
                if (site.id > 0) {
                    newSite.id = site.id
                }
                newSite
            }

            is Error -> {
                val payload = SiteModel()
                payload.error = response.error
                payload
            }
        }
    }

    private suspend fun fetchSiteUsingRootEndpoint(site: SiteModel): SiteModel {
        val result = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            restClient = this,
            site = site,
            url = "/",
            params = mapOf("_fields" to ROOT_ENDPOINT_FIELDS),
            clazz = RootWPAPIRestResponse::class.java
        )

        return when {
            result is JetpackResponse.JetpackSuccess && result.data != null -> {
                // Keep existing fields, and update only fields fetched from the root endpoint
                site.apply {
                    name = result.data.name
                    timezone = result.data.gmtOffset
                    hasWooCommerce = result.data.namespaces?.any {
                        it.startsWith(WOO_API_NAMESPACE_PREFIX)
                    } ?: false

                    applicationPasswordsAuthorizeUrl = result.data.authentication?.applicationPasswords
                        ?.endpoints?.authorization
                }
            }

            else -> {
                val payload = SiteModel()
                payload.error = (result as? JetpackResponse.JetpackError)?.error
                    ?: BaseNetworkError(GenericErrorType.UNKNOWN)
                payload
            }
        }
    }

    private suspend fun fetchAppPasswordsAuthorizationUrl(site: SiteModel): Result<String?> {
        val result = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            restClient = this,
            site = site,
            url = "/",
            params = mapOf("_fields" to "authentication"),
            clazz = RootWPAPIRestResponse::class.java
        )

        return (result as? JetpackResponse.JetpackSuccess)?.let {
            Result.success(it.data?.authentication?.applicationPasswords?.endpoints?.authorization)
        } ?: Result.failure(Exception((result as? JetpackResponse.JetpackError)?.error?.message ?: "Unknown error"))
    }

    /**
     * Calls the API at https://public-api.wordpress.com/rest/v1.1/sites/new/ to create a new site
     * @param siteName The domain of the site
     * @param siteTitle The title of the site
     * @param language The language of the site
     * @param timeZoneId The timezone of the site
     * @param visibility The visibility of the site (public or private)
     * @param segmentId The segment that the site belongs to
     * @param siteDesign The design template of the site
     * @param isComingSoon The "coming soon" flag, which hides the site content from the public
     * @param dryRun If set to true the call only validates the parameters passed
     *
     * The domain of the site is generated with the following logic:
     *
     * 1. If the [siteName] is provided it is used as a domain
     * 2. If the [siteName] is not provided the [siteTitle] is passed and the API generates the domain from it
     * 3. If neither the [siteName] or the [siteTitle] is passed the api generates a domain of the form siteXXXXXX
     *
     * In the cases 2 and 3 two extra parameters are passed:
     * - `options.site_creation_flow` with value `with-design-picker`
     * - `find_available_url` with value `1`
     *
     * @return the response of the API call  as [NewSiteResponsePayload]
     */
    suspend fun launchSite(site: SiteModel): Response<Unit> {
        val url = WPCOMV2.sites.site(site.siteId).launch.url
        return wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = mapOf(),
            body = mapOf("site" to site.siteId),
            Unit::class.java
        )
    }

    @Suppress("LongParameterList")
    fun suggestDomains(
        query: String,
        quantity: Int,
        vendor: String?,
        onlyWordpressCom: Boolean?,
        includeWordpressCom: Boolean?,
        includeDotBlogSubdomain: Boolean?,
        segmentId: Long?,
        tlds: String?
    ) {
        val url = WPCOMREST.domains.suggestions.urlV1_1
        val params = mutableMapOf<String, String>()
        params["query"] = query
        params["quantity"] = quantity.toString()
        if (vendor != null) {
            params["vendor"] = vendor
        }
        if (onlyWordpressCom != null) {
            params["only_wordpressdotcom"] = onlyWordpressCom.toString() // CHECKSTYLE IGNORE
        }
        if (includeWordpressCom != null) {
            params["include_wordpressdotcom"] = includeWordpressCom.toString() // CHECKSTYLE IGNORE
        }
        if (includeDotBlogSubdomain != null) {
            params["include_dotblogsubdomain"] = includeDotBlogSubdomain.toString()
        }
        if (segmentId != null) {
            params["segment_id"] = segmentId.toString()
        }
        if (tlds != null) {
            params["tlds"] = tlds
        }
        val request = WPComGsonRequest.buildGetRequest<List<DomainSuggestionResponse>>(url, params,
                object : TypeToken<List<DomainSuggestionResponse>>() {}.type,
                { response, _ ->
                    val payload = SuggestDomainsResponsePayload(
                            query,
                            response
                    )
                    mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload))
                },
                { error ->
                    val suggestDomainError = SuggestDomainError(error.apiError, error.message)
                    if (suggestDomainError.type === EMPTY_RESULTS) {
                        // Empty results is not an actual error, the API should return 200 for it
                        val payload = SuggestDomainsResponsePayload(query, emptyList())
                        mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload))
                    } else {
                        val payload = SuggestDomainsResponsePayload(query, suggestDomainError)
                        mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload))
                    }
                }
        )
        add(request)
    }

    // Unauthenticated network calls
    fun fetchConnectSiteInfo(siteUrl: String) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchConnectSiteInfo") {
            fetchConnectSiteInfoSync(siteUrl).let { payload ->
                mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(payload))
            }
        }
    }

    @Suppress("SwallowedException")
    suspend fun fetchConnectSiteInfoSync(siteUrl: String): ConnectSiteInfoPayload {
        fun ConnectSiteInfoResponse.toConnectSiteInfoPayload(url: String): ConnectSiteInfoPayload {
            return ConnectSiteInfoPayload(
                url,
                exists,
                isWordPress,
                hasJetpack,
                isJetpackActive,
                isJetpackConnected,
                isWordPressDotCom, // CHECKSTYLE IGNORE
                isCommerceGarden,
                urlAfterRedirects
            )
        }

        // Get a proper URI to reliably retrieve the scheme.
        val uri: URI = try {
            URI.create(UrlUtils.addUrlSchemeIfNeeded(siteUrl, false))
        } catch (e: IllegalArgumentException) {
            val siteError = SiteError(INVALID_SITE)
            return ConnectSiteInfoPayload(siteUrl, siteError)
        }

        val params = mutableMapOf<String, String>()
        params["url"] = uri.toString()

        // Make the call.
        val url = WPCOMREST.connect.site_info.urlV1_1
        val response = wpComGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            params = params,
            clazz = ConnectSiteInfoResponse::class.java
        )

        return when (response) {
            is Error -> {
                val siteErrorType = when (response.error.apiError) {
                    "connection_disabled" -> SiteErrorType.WPCOM_SITE_SUSPENDED
                    else -> {
                        when {
                            isTlsCertificateValidityIssue(response.error) -> TLS_CERTIFICATE_VALIDITY_ERROR
                            isRemoteSiteCertificateIssue(response.error) -> REMOTE_SITE_CERTIFICATE_ERROR
                            isWordPressComConnectivityIssue(response.error) -> WORDPRESS_COM_CONNECTIVITY_ERROR
                            else -> INVALID_SITE
                        }
                    }
                }

                ConnectSiteInfoPayload(siteUrl, SiteError(siteErrorType))
            }
            is Success -> {
                response.data.toConnectSiteInfoPayload(siteUrl)
            }
        }
    }

    private fun isTlsCertificateValidityIssue(error: WPComGsonNetworkError): Boolean {
        if (error.type !in TLS_CERTIFICATE_VALIDITY_ERROR_TYPES) {
            return false
        }

        return error.volleyError.hasCertificateValidityIssue() ||
            error.getCombinedErrorMessage().isCertificateValidityMessage()
    }

    private fun isRemoteSiteCertificateIssue(error: WPComGsonNetworkError): Boolean {
        return error.apiError == "follow_redirects_failed" &&
            error.getCombinedErrorMessage().contains("curl error 60", ignoreCase = true)
    }

    private fun Throwable?.hasCertificateValidityIssue(): Boolean =
        generateSequence(this) { it.cause }.any { throwable ->
            throwable is CertificateExpiredException ||
                throwable is CertificateNotYetValidException ||
                throwable.message.isCertificateValidityMessage() ||
                throwable.isAndroidCertificateValidityException()
        }

    private fun Throwable.isAndroidCertificateValidityException(): Boolean {
        return this is java.security.cert.CertificateException &&
            message?.contains("unacceptable certificate", ignoreCase = true) == true &&
            // Android Conscrypt can wrap date validity failures as a generic platform CertificateException.
            // Cause/message matching above remains the portable fallback for other TLS providers.
            stackTrace.any { stackTraceElement ->
                stackTraceElement.className == "com.android.org.conscrypt.OpenSSLX509Certificate" &&
                    stackTraceElement.methodName == "checkValidity"
            }
    }

    private fun String?.isCertificateValidityMessage(): Boolean {
        return this?.contains("certificate has expired", ignoreCase = true) == true ||
            this?.contains("certificate expired", ignoreCase = true) == true ||
            this?.contains("not yet valid", ignoreCase = true) == true
    }

    /**
     * Determines if a network error indicates a WordPress.com connectivity issue
     * rather than an invalid site URL.
     */
    private fun isWordPressComConnectivityIssue(error: WPComGsonNetworkError): Boolean {
        return when (error.type) {
            GenericErrorType.NO_CONNECTION,
            GenericErrorType.NETWORK_ERROR,
            GenericErrorType.TIMEOUT -> {
                error.volleyError?.cause?.let { cause ->
                    when (cause) {
                        is UnknownHostException -> {
                            cause.message?.contains("public-api.wordpress.com", ignoreCase = true) == true
                        }
                        else -> {
                            error.volleyError.message?.contains("public-api.wordpress.com", ignoreCase = true) == true
                        }
                    }
                } ?: false
            }
            else -> false
        }
    }

    /**
     * Performs an HTTP GET call to v1.1 /domains/supported-states/$countryCode endpoint. Upon receiving a response
     * (success or error) a [SiteAction.FETCHED_DOMAIN_SUPPORTED_STATES] action is dispatched with a
     * payload of type [DomainSupportedStatesResponsePayload].
     *
     * [DomainSupportedStatesResponsePayload.isError] can be used to check the request result.
     */
    fun fetchSupportedStates(countryCode: String) {
        val url = WPCOMREST.domains.supported_states.countryCode(countryCode).urlV1_1
        val request = WPComGsonRequest.buildGetRequest<List<SupportedStateResponse>>(url, null,
                object : TypeToken<List<SupportedStateResponse>>() {}.type,
                { response, _ ->
                    val payload = DomainSupportedStatesResponsePayload(response)
                    mDispatcher.dispatch(SiteActionBuilder.newFetchedDomainSupportedStatesAction(payload))
                },
                { error ->
                    val domainSupportedStatesError = DomainSupportedStatesError(
                            DomainSupportedStatesErrorType.fromString(error.apiError), error.message
                    )
                    val payload = DomainSupportedStatesResponsePayload(domainSupportedStatesError)
                    mDispatcher.dispatch(SiteActionBuilder.newFetchedDomainSupportedStatesAction(payload))
                })
        add(request)
    }

    suspend fun fetchSiteDomains(site: SiteModel): Response<DomainsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).domains.urlV1_1
        return wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), DomainsResponse::class.java)
    }

    suspend fun fetchSitePlans(site: SiteModel): Response<PlansResponse> {
        val url = WPCOMREST.sites.site(site.siteId).plans.urlV1_3
        return wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), PlansResponse::class.java)
    }

    suspend fun fetchDomainPrice(domainName: String): Response<DomainPriceResponse> {
        val url = WPCOMREST.domains.domainName(domainName).price.urlV1_1
        return wpComGsonRequestBuilder.syncGetRequest(this, url, mapOf(), DomainPriceResponse::class.java)
    }

    fun designatePrimaryDomain(site: SiteModel, domain: String) {
        val url = WPCOMREST.sites.site(site.siteId).domains.primary.urlV1_1
        val params = mutableMapOf<String, Any>()
        params["domain"] = domain
        val request = WPComGsonRequest
                .buildPostRequest(url, params, DesignatePrimaryDomainResponse::class.java,
                        { (success), _ ->
                            mDispatcher.dispatch(
                                    SiteActionBuilder.newDesignatedPrimaryDomainAction(
                                            DesignatedPrimaryDomainPayload(site, success)
                                    )
                            )
                        }
                ) { networkError ->
                    val error = DesignatePrimaryDomainError(
                            DesignatePrimaryDomainErrorType.GENERIC_ERROR, networkError.message
                    )
                    val payload = DesignatedPrimaryDomainPayload(site, false)
                    payload.error = error
                    mDispatcher.dispatch(SiteActionBuilder.newDesignatedPrimaryDomainAction(payload))
                }
        add(request)
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun siteResponseToSiteModel(from: SiteWPComRestResponse): SiteModel {
        val site = SiteModel()
        site.siteId = from.ID
        site.url = from.URL
        site.name = StringEscapeUtils.unescapeHtml4(from.name)
        site.setIsJetpackConnected(from.jetpack && from.jetpack_connection)
        site.setIsJetpackInstalled(from.jetpack)
        site.setIsJetpackCPConnected(from.jetpack_connection && !from.jetpack)
        site.setIsPrivate(from.is_private)
        // Depending of user's role, options could be "hidden", for instance an "Author" can't read site options.
        if (from.options != null) {
            site.setIsWpComStore(from.options.is_wpcom_store)
            site.publishedStatus = from.options.blog_public
            site.hasWooCommerce = from.options.woocommerce_is_active
            site.adminUrl = from.options.admin_url
            site.loginUrl = from.options.login_url
            site.timezone = from.options.gmt_offset
            site.jetpackVersion = from.options.jetpack_version
            site.setIsWPComAtomic(from.options.is_wpcom_atomic)
            site.canBlaze = from.options.can_blaze
            from.options.jetpack_connection_active_plugins?.let {
                site.activeJetpackConnectionPlugins = it.joinToString(",")
            }
            from.jetpack_modules?.let {
                site.jetpackModules = it.joinToString(",")
            }
        }
        if (from.plan != null) {
            try {
                site.planId = java.lang.Long.valueOf(from.plan.product_id)
            } catch (e: NumberFormatException) {
                // VIP sites return a String plan ID ('vip') rather than a number
                if (from.plan.product_id == "vip") {
                    site.planId = SiteModel.VIP_PLAN_ID
                }
            }
            site.planShortName = from.plan.product_name_short
            site.planProductSlug = from.plan.product_slug
        }
        if (from.capabilities != null) {
            site.hasCapabilityManageOptions = from.capabilities.manage_options
        }
        if (from.meta != null) {
            if (from.meta.links != null) {
                site.xmlRpcUrl = from.meta.links.xmlrpc
            }
        }
        // Only set the isWPCom flag for "pure" WPCom sites
        if (!from.jetpack_connection) {
            site.setIsWPCom(true)
        }
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        site.planActiveFeatures = (from.plan?.features?.active?.joinToString(",")).orEmpty()

        site.setIsGardenSite(from.is_garden)
        site.gardenName = from.garden_name
        site.gardenPartner = from.garden_partner

        // CIAB sites always have WooCommerce, even if the API reports otherwise
        if (from.is_garden && from.garden_name == SiteModel.CIAB_GARDEN_NAME) {
            site.hasWooCommerce = true
        }

        return site
    }

    companion object {
        @VisibleForTesting
        const val SITE_FIELDS = "ID,URL,name,jetpack,jetpack_connection,is_private," +
            "options,plan,capabilities,meta,jetpack_modules,is_garden,garden_name,garden_partner"
        private const val ROOT_ENDPOINT_FIELDS = "name,gmt_offset,namespaces,authentication"
        private const val WOO_API_NAMESPACE_PREFIX = "wc/"
        private const val FIELDS = "fields"
        private const val FILTERS = "filters"
    }
}
