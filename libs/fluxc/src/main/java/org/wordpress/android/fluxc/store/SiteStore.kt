package org.wordpress.android.fluxc.store

import android.text.TextUtils
import com.wellsql.generated.SiteModelTable
import dagger.Lazy
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATED_PRIMARY_DOMAIN
import org.wordpress.android.fluxc.action.SiteAction.DESIGNATE_PRIMARY_DOMAIN
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_CONNECT_SITE_INFO
import org.wordpress.android.fluxc.action.SiteAction.FETCHED_DOMAIN_SUPPORTED_STATES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_CONNECT_SITE_INFO
import org.wordpress.android.fluxc.action.SiteAction.FETCH_DOMAIN_SUPPORTED_STATES
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITE
import org.wordpress.android.fluxc.action.SiteAction.FETCH_SITES
import org.wordpress.android.fluxc.action.SiteAction.REMOVE_ALL_SITES
import org.wordpress.android.fluxc.action.SiteAction.REMOVE_SITE
import org.wordpress.android.fluxc.action.SiteAction.SUGGESTED_DOMAINS
import org.wordpress.android.fluxc.action.SiteAction.SUGGEST_DOMAINS
import org.wordpress.android.fluxc.action.SiteAction.UPDATE_SITE
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.logging.FluxCCrashLogger
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.model.asDomainModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordDeletionResult
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsManager
import org.wordpress.android.fluxc.network.rest.wpapi.site.SiteWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainPriceResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.SiteStorePersistence
import org.wordpress.android.fluxc.persistence.domains.DomainDao
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesErrorType.INVALID_COUNTRY_CODE
import org.wordpress.android.fluxc.store.SiteStore.LaunchSiteErrorType.ALREADY_LAUNCHED
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType.NOT_AVAILABLE
import org.wordpress.android.fluxc.store.SiteStore.SelfHostedErrorType.NOT_SET
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.DUPLICATE_SITE
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.UNAUTHORIZED
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.UNKNOWN_SITE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.SiteErrorUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * SQLite based only. There is no in memory copy of mapped data, everything is queried from the DB.
 *
 * NOTE: This class needs to be open because it's mocked in android tests in the WPAndroid project.
 *       TODO: consider adding https://kotlinlang.org/docs/all-open-plugin.html
 */
@Suppress("LargeClass", "TooManyFunctions", "LongParameterList", "ForbiddenComment")
@Singleton
open class SiteStore @Inject constructor(
    dispatcher: Dispatcher?,
    private val siteRestClient: SiteRestClient,
    private val siteWPAPIRestClient: SiteWPAPIRestClient,
    private val siteSqlUtils: SiteSqlUtils,
    private val siteStorePersistence: SiteStorePersistence,
    private val domainDao: DomainDao,
    private val coroutineEngine: CoroutineEngine,
    private val crashLogger: Lazy<FluxCCrashLogger>
) : Store(dispatcher) {
    @Inject internal lateinit var applicationPasswordsManagerProvider: Provider<ApplicationPasswordsManager>

    // Payloads
    data class FetchWPAPISitePayload(
        val url: String,
        val username: String? = null,
        val password: String? = null,
    ) : Payload<BaseNetworkError>()

    data class FetchSitesPayload @JvmOverloads constructor(
        @JvmField val filters: List<SiteFilter> = ArrayList(),
        @JvmField val filterJetpackConnectedPackageSite: Boolean = false
    ) : Payload<BaseNetworkError>()

    data class FetchedPlansPayload(
        @JvmField val site: SiteModel,
        @JvmField val plans: List<PlanModel>? = null
    ) : Payload<PlansError>() {
        constructor(site: SiteModel, error: PlansError) : this(site) {
            this.error = error
        }
    }

    data class SuggestDomainsPayload(
        @JvmField val query: String,
        @JvmField val quantity: Int,
        @JvmField val vendor: String? = null,
        @JvmField val onlyWordpressCom: Boolean? = null,
        @JvmField val includeWordpressCom: Boolean? = null,
        @JvmField val includeDotBlogSubdomain: Boolean? = null,
        @JvmField val tlds: String? = null,
        @JvmField val segmentId: Long? = null
    ) : Payload<BaseNetworkError>()

    data class SuggestDomainsResponsePayload(
        @JvmField val query: String,
        @JvmField val suggestions: List<DomainSuggestionResponse> = listOf()
    ) : Payload<SuggestDomainError>() {
        constructor(query: String, error: SuggestDomainError?) : this(query) {
            this.error = error
        }
    }

    data class FetchConnectSiteInfoPayload @JvmOverloads constructor(
        @JvmField val siteUrl: String,
        @JvmField val discoverWPAPIOnFailure: Boolean = false
    ) : Payload<BaseNetworkError>()

    data class ConnectSiteInfoPayload @JvmOverloads constructor(
        @JvmField val url: String,
        @JvmField val exists: Boolean = false,
        @JvmField val isWordPress: Boolean = false,
        @JvmField val hasJetpack: Boolean = false,
        @JvmField val isJetpackActive: Boolean = false,
        @JvmField val isJetpackConnected: Boolean = false,
        @JvmField val isWPCom: Boolean = false,
        @JvmField val urlAfterRedirects: String? = null
    ) : Payload<SiteError>() {
        constructor(url: String, error: SiteError?) : this(url) {
            this.error = error
        }

        fun description(): String {
            return String.format(
                Locale.US,
                "url: %s, e: %b, wp: %b, jp: %b, wpcom: %b, urlAfterRedirects: %s",
                url, exists, isWordPress, hasJetpack, isWPCom, urlAfterRedirects
            )
        }
    }

    data class WPAPIDiscoveryResult @JvmOverloads constructor(
        @JvmField val connectSiteInfoApiError: String? = null,
        @JvmField val wpApiBaseUrl: String? = null
    )

    data class DesignatePrimaryDomainPayload(
        @JvmField val site: SiteModel,
        @JvmField val domain: String
    ) : Payload<DesignatePrimaryDomainError>()

    data class DomainSupportedStatesResponsePayload(
        @JvmField val supportedStates: List<SupportedStateResponse>? = null
    ) : Payload<DomainSupportedStatesError>() {
        constructor(error: DomainSupportedStatesError) : this() {
            this.error = error
        }
    }

    data class SiteError @JvmOverloads constructor(
        @JvmField val type: SiteErrorType,
        @JvmField val message: String? = null,
        @JvmField val selfHostedErrorType: SelfHostedErrorType = NOT_SET,
        @JvmField val wpApiDiscovery: WPAPIDiscoveryResult? = null
    ) : OnChangedError

    data class DomainSupportedStatesError
    @JvmOverloads
    constructor(
        @JvmField val type: DomainSupportedStatesErrorType,
        @JvmField val message: String? = null
    ) : OnChangedError

    data class DesignatePrimaryDomainError(
        @JvmField val type: DesignatePrimaryDomainErrorType,
        @JvmField val message: String?
    ) : OnChangedError

    // OnChanged Events
    data class OnSiteChanged(
        @JvmField val rowsAffected: Int = 0,
        @JvmField val updatedSites: List<SiteModel> = emptyList()
    ) : OnChanged<SiteError>() {
        constructor(rowsAffected: Int = 0, siteError: SiteError?) : this(rowsAffected) {
            this.error = siteError
        }

        constructor(siteError: SiteError) : this(0, siteError)
    }

    data class OnSiteRemoved(@JvmField val mRowsAffected: Int) : OnChanged<SiteError>()
    data class OnAllSitesRemoved(@JvmField val mRowsAffected: Int) : OnChanged<SiteError>()

    data class OnConnectSiteInfoChecked(@JvmField val info: ConnectSiteInfoPayload) : OnChanged<SiteError>()

    data class SuggestDomainError(@JvmField val type: SuggestDomainErrorType, @JvmField val message: String) :
            OnChangedError {
        constructor(apiErrorType: String, message: String) : this(
                SuggestDomainErrorType.fromString(apiErrorType),
                message
        )
    }

    data class OnSuggestedDomains(
        val query: String,
        @JvmField val suggestions: List<DomainSuggestionResponse>
    ) : OnChanged<SuggestDomainError>()

    data class OnDomainSupportedStatesFetched(
        @JvmField val supportedStates: List<SupportedStateResponse>?
    ) : OnChanged<DomainSupportedStatesError>() {
        constructor(
            supportedStates: List<SupportedStateResponse>?,
            error: DomainSupportedStatesError?
        ) : this(supportedStates) {
            this.error = error
        }
    }

    data class FetchedDomainsPayload(
        @JvmField val site: SiteModel,
        @JvmField val domains: List<Domain>? = null
    ) : Payload<SiteError>() {
        constructor(site: SiteModel, error: SiteError) : this(site) {
            this.error = error
        }
    }

    data class OnApplicationPasswordDeleted(val site: SiteModel) : OnChanged<OnApplicationPasswordDeleteError>() {
        constructor(site: SiteModel, error: BaseNetworkError) : this(site) {
            this.error = OnApplicationPasswordDeleteError(error)
        }
    }

    class OnSiteLaunched() : OnChanged<LaunchSiteError>() {
        constructor(error: LaunchSiteError) : this() {
            this.error = error
        }
    }

    data class LaunchSiteError internal constructor(
        @JvmField val type: LaunchSiteErrorType?,
        @JvmField val message: String
    ) : OnChangedError

    enum class LaunchSiteErrorType {
        GENERIC_ERROR,
        ALREADY_LAUNCHED,
        UNAUTHORIZED
    }

    class OnApplicationPasswordDeleteError(error: BaseNetworkError) : OnChangedError {
        var errorCode: String? = null
        var message: String

        init {
            if (error is WPAPINetworkError) {
                errorCode = error.errorCode
            } else if (error is WPComGsonNetworkError) {
                errorCode = error.apiError
            }
            message = error.message
        }
    }

    class PlansError
    @JvmOverloads constructor(
        @JvmField val type: PlansErrorType,
        @JvmField val message: String? = null
    ) : OnChangedError

    class DesignatedPrimaryDomainPayload(
        @JvmField val site: SiteModel,
        @JvmField val success: Boolean
    ) : OnChanged<DesignatePrimaryDomainError>()

    class OnPrimaryDomainDesignated(
        @JvmField val site: SiteModel,
        @JvmField val success: Boolean
    ) : OnChanged<DesignatePrimaryDomainError>()

    data class UpdateSitesResult(
        @JvmField val rowsAffected: Int = 0,
        @JvmField val updatedSites: List<SiteModel> = emptyList(),
        @JvmField val duplicateSiteFound: Boolean = false
    )

    enum class SiteErrorType {
        INVALID_SITE,
        UNKNOWN_SITE,
        DUPLICATE_SITE,
        INVALID_RESPONSE,
        UNAUTHORIZED,
        NOT_AUTHENTICATED,
        GENERIC_ERROR,
        WPCOM_SITE_SUSPENDED,
        TLS_CERTIFICATE_VALIDITY_ERROR,
        REMOTE_SITE_CERTIFICATE_ERROR,
        WORDPRESS_COM_CONNECTIVITY_ERROR
    }

    enum class SuggestDomainErrorType {
        EMPTY_RESULTS, EMPTY_QUERY, INVALID_MINIMUM_QUANTITY, INVALID_MAXIMUM_QUANTITY, INVALID_QUERY, GENERIC_ERROR;

        companion object {
            fun fromString(string: String): SuggestDomainErrorType {
                if (!TextUtils.isEmpty(string)) {
                    for (v in values()) {
                        if (string.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class PlansErrorType {
        NOT_AVAILABLE, AUTHORIZATION_REQUIRED, UNAUTHORIZED, UNKNOWN_BLOG, GENERIC_ERROR
    }

    enum class SelfHostedErrorType {
        NOT_SET,
        XML_RPC_SERVICES_DISABLED,
        UNABLE_TO_READ_SITE
    }

    // Enums
    enum class DomainSupportedStatesErrorType {
        INVALID_COUNTRY_CODE, INVALID_QUERY, GENERIC_ERROR;

        companion object {
            @JvmStatic fun fromString(type: String): DomainSupportedStatesErrorType {
                if (!TextUtils.isEmpty(type)) {
                    for (v in values()) {
                        if (type.equals(v.name, ignoreCase = true)) {
                            return v
                        }
                    }
                }
                return GENERIC_ERROR
            }
        }
    }

    enum class DesignatePrimaryDomainErrorType {
        GENERIC_ERROR
    }

    enum class SiteVisibility(private val mValue: Int) {
        PRIVATE(-1),
        BLOCK_SEARCH_ENGINE(0),
        PUBLIC(1),
        COMING_SOON(999);

        fun value(): Int {
            return mValue
        }
    }

    enum class SiteFilter(private val mString: String) {
        ATOMIC("atomic"), JETPACK("jetpack"), WPCOM("wpcom");

        override fun toString(): String {
            return mString
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, "SiteStore onRegister")
    }

    /**
     * Returns all sites in the store as a [SiteModel] list.
     */
    val sites: List<SiteModel>
        get() = siteSqlUtils.getSites()

    /**
     * Obtains the site with the given (local) id and returns it as a [SiteModel].
     */
    fun getSiteByLocalId(id: Int) = siteSqlUtils.getSitesWithLocalId(id).firstOrNull()

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API).
     */
    val sitesAccessedViaWPComRest: List<SiteModel>
        get() = siteSqlUtils.sitesAccessedViaWPComRest.asModel

    /**
     * Returns sites with a name or url matching the search string.
     */
    fun getSitesByNameOrUrlMatching(searchString: String): List<SiteModel> {
        return siteSqlUtils.getSitesByNameOrUrlMatching(searchString)
    }

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API) with a
     * name or url matching the search string.
     */
    fun getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString: String): List<SiteModel> {
        return siteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString)
    }

    /**
     * Given a .COM site ID (either a .COM site id, or the .COM id of a Jetpack site), returns the site as a
     * [SiteModel].
     */
    fun getSiteBySiteId(siteId: Long): SiteModel? {
        if (siteId == 0L) {
            return null
        }
        val sites = siteSqlUtils.getSitesWithRemoteId(siteId)
        return if (sites.isEmpty()) {
            null
        } else {
            sites[0]
        }
    }

    @Throws(SiteStorePersistence.DuplicateSiteException::class)
    fun insertOrUpdateSite(site: SiteModel): Int = siteStorePersistence.insertOrUpdateSite(site)

    fun getWooCommerceSites(): List<SiteModel> =
        siteSqlUtils.getSitesWith(SiteModelTable.HAS_WOO_COMMERCE, true).asModel

    @Subscribe(threadMode = ASYNC)
    @Suppress("LongMethod", "ComplexMethod")
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? SiteAction ?: return
        when (actionType) {
            FETCH_SITE -> coroutineEngine.launch(T.MAIN, this, "Fetch site") {
                emitChange(fetchSite(action.payload as SiteModel))
            }
            FETCH_SITES -> coroutineEngine.launch(T.MAIN, this, "Fetch sites") {
                emitChange(fetchSites(action.payload as FetchSitesPayload))
            }
            UPDATE_SITE -> {
                emitChange(updateSite(action.payload as SiteModel))
            }
            REMOVE_SITE -> removeSite(action.payload as SiteModel)
            REMOVE_ALL_SITES -> removeAllSites()
            FETCH_CONNECT_SITE_INFO -> fetchConnectSiteInfo(action.payload as FetchConnectSiteInfoPayload)
            FETCHED_CONNECT_SITE_INFO -> handleFetchedConnectSiteInfo(action.payload as ConnectSiteInfoPayload)
            SUGGEST_DOMAINS -> suggestDomains(action.payload as SuggestDomainsPayload)
            SUGGESTED_DOMAINS -> handleSuggestedDomains(action.payload as SuggestDomainsResponsePayload)
            FETCH_DOMAIN_SUPPORTED_STATES -> fetchSupportedStates(action.payload as String)
            FETCHED_DOMAIN_SUPPORTED_STATES -> handleFetchedSupportedStates(
                    action.payload as DomainSupportedStatesResponsePayload
            )
            DESIGNATE_PRIMARY_DOMAIN -> designatePrimaryDomain(action.payload as DesignatePrimaryDomainPayload)
            DESIGNATED_PRIMARY_DOMAIN -> handleDesignatedPrimaryDomain(action.payload as DesignatedPrimaryDomainPayload)
        }
    }

    suspend fun fetchSite(site: SiteModel): OnSiteChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "Fetch site") {
            when (site.origin) {
                SiteModel.ORIGIN_WPCOM_REST -> updateSite(siteRestClient.fetchSite(site))
                SiteModel.ORIGIN_WPAPI -> updateSite(siteWPAPIRestClient.fetchWPAPISite(site))
                else -> {
                    reportXmlrpcTry()
                    OnSiteChanged(SiteError(SiteErrorType.GENERIC_ERROR))
                }
            }
        }
    }

    suspend fun fetchSites(payload: FetchSitesPayload): OnSiteChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "Fetch sites") {
            val result = siteRestClient.fetchSites(payload.filters, payload.filterJetpackConnectedPackageSite)
            handleFetchedSitesWPComRest(result)
        }
    }

    suspend fun fetchWPAPISite(payload: FetchWPAPISitePayload): OnSiteChanged {
        return coroutineEngine.withDefaultContext(T.MAIN, this, "Fetch WPAPI Site") {
            updateSite(siteWPAPIRestClient.fetchWPAPISite(payload))
        }
    }

    @Suppress("ForbiddenComment", "SwallowedException")
    private fun updateSite(siteModel: SiteModel): OnSiteChanged {
        return if (siteModel.isError) {
            // TODO: what kind of error could we get here?
            OnSiteChanged(SiteErrorUtils.genericToSiteError(siteModel.error))
        } else {
            try {
                OnSiteChanged(createOrUpdateSite(siteModel, includesAppPasswordsUrl = true))
            } catch (e: SiteStorePersistence.DuplicateSiteException) {
                OnSiteChanged(SiteError(DUPLICATE_SITE))
            }
        }
    }

    @Suppress("ForbiddenComment")
    private fun handleFetchedSitesWPComRest(fetchedSites: SitesModel): OnSiteChanged {
        return if (fetchedSites.isError) {
            // TODO: what kind of error could we get here?
            OnSiteChanged(SiteErrorUtils.genericToSiteError(fetchedSites.error))
        } else {
            val res = createOrUpdateSites(fetchedSites)
            val result = if (res.duplicateSiteFound) {
                OnSiteChanged(res.rowsAffected, SiteError(DUPLICATE_SITE))
            } else {
                OnSiteChanged(res.rowsAffected, res.updatedSites)
            }
            siteSqlUtils.removeWPComRestSitesAbsentFromList(fetchedSites.sites)

            result
        }
    }

    @Suppress("SwallowedException")
    private fun createOrUpdateSites(sites: SitesModel): UpdateSitesResult {
        var rowsAffected = 0
        var duplicateSiteFound = false
        val updatedSites = mutableListOf<SiteModel>()
        for (site in sites.sites) {
            try {
                val isUpdated = (createOrUpdateSite(site, includesAppPasswordsUrl = false) == 1)
                if (isUpdated) {
                    rowsAffected++
                    updatedSites.add(site)
                }
            } catch (e: SiteStorePersistence.DuplicateSiteException) {
                duplicateSiteFound = true
            }
        }
        return UpdateSitesResult(rowsAffected, updatedSites, duplicateSiteFound)
    }

    private fun createOrUpdateSite(site: SiteModel, includesAppPasswordsUrl: Boolean): Int {
        val freshSiteFromDB = getSiteBySiteId(site.siteId)
        // Update the site with existing values from the DB that are not returned by the REST API
        if (freshSiteFromDB != null) {
            // The WPCom REST API doesn't return info about the application passwords authorize URL.
            if (site.origin == SiteModel.ORIGIN_WPCOM_REST && !includesAppPasswordsUrl) {
                site.applicationPasswordsAuthorizeUrl = freshSiteFromDB.applicationPasswordsAuthorizeUrl
            }
        }

        return insertOrUpdateSite(site)
    }

    private fun removeSite(site: SiteModel) {
        val rowsAffected = siteSqlUtils.deleteSite(site)
        emitChange(OnSiteRemoved(rowsAffected))
    }

    private fun removeAllSites() {
        val rowsAffected = siteSqlUtils.deleteAllSites()
        val event = OnAllSitesRemoved(rowsAffected)

        emitChange(event)
    }

    private fun fetchConnectSiteInfo(payload: FetchConnectSiteInfoPayload) {
        siteRestClient.fetchConnectSiteInfo(payload.siteUrl, payload.discoverWPAPIOnFailure)
    }

    suspend fun fetchConnectSiteInfoSync(
        siteUrl: String,
        discoverWPAPIOnFailure: Boolean = false
    ): ConnectSiteInfoPayload {
        return coroutineEngine.withDefaultContext(T.API, this, "Fetch Connect Site Info") {
            siteRestClient.fetchConnectSiteInfoSync(siteUrl, discoverWPAPIOnFailure)
        }
    }

    private fun handleFetchedConnectSiteInfo(payload: ConnectSiteInfoPayload) {
        val event = OnConnectSiteInfoChecked(payload)
        event.error = payload.error
        emitChange(event)
    }

    private fun suggestDomains(payload: SuggestDomainsPayload) {
        siteRestClient.suggestDomains(
            payload.query,
            payload.quantity,
            payload.vendor,
            payload.onlyWordpressCom,
            payload.includeWordpressCom,
            payload.includeDotBlogSubdomain,
            payload.segmentId,
            payload.tlds
        )
    }

    private fun handleSuggestedDomains(payload: SuggestDomainsResponsePayload) {
        val event = OnSuggestedDomains(payload.query, payload.suggestions)
        if (payload.isError) {
            event.error = payload.error
        }
        emitChange(event)
    }

    private fun fetchSupportedStates(countryCode: String) {
        if (TextUtils.isEmpty(countryCode)) {
            val error = DomainSupportedStatesError(INVALID_COUNTRY_CODE)
            handleFetchedSupportedStates(DomainSupportedStatesResponsePayload(error))
        } else {
            siteRestClient.fetchSupportedStates(countryCode)
        }
    }

    private fun handleFetchedSupportedStates(payload: DomainSupportedStatesResponsePayload) {
        emitChange(OnDomainSupportedStatesFetched(payload.supportedStates, payload.error))
    }

    private fun designatePrimaryDomain(payload: DesignatePrimaryDomainPayload) {
        siteRestClient.designatePrimaryDomain(payload.site, payload.domain)
    }

    private fun handleDesignatedPrimaryDomain(payload: DesignatedPrimaryDomainPayload) {
        val event = OnPrimaryDomainDesignated(payload.site, payload.success)
        event.error = payload.error
        emitChange(event)
    }

    suspend fun fetchSiteDomains(siteModel: SiteModel): FetchedDomainsPayload =
            coroutineEngine.withDefaultContext(T.API, this, "Fetch site domains") {
                return@withDefaultContext when (val response =
                        siteRestClient.fetchSiteDomains(siteModel)) {
                            is Success -> {
                                val domains = response.data.domains
                                insertDomainModels(siteModel, domains)
                                FetchedDomainsPayload(siteModel, domains)
                            }
                            is Error -> {
                                val siteErrorType = when (response.error.apiError) {
                                    "unauthorized" -> UNAUTHORIZED
                                    "unknown_blog" -> UNKNOWN_SITE
                                    else -> SiteErrorType.GENERIC_ERROR
                                }
                                val domainsError = SiteError(siteErrorType, response.error.message)
                                FetchedDomainsPayload(siteModel, domainsError)
                            }
                }
            }

    private suspend fun insertDomainModels(siteModel: SiteModel, domains: List<Domain>) {
        val domainModels = domains.map { it.asDomainModel() }
        domainDao.insert(siteModel.id, domainModels)
    }

    suspend fun deleteApplicationPassword(site: SiteModel): OnApplicationPasswordDeleted =
        coroutineEngine.withDefaultContext(T.API, this, "Delete Application Password") {
            when (val result = applicationPasswordsManagerProvider.get().deleteApplicationCredentials(site)) {
                is ApplicationPasswordDeletionResult.Success -> OnApplicationPasswordDeleted(site)
                is ApplicationPasswordDeletionResult.Failure -> OnApplicationPasswordDeleted(site, result.error)
            }
        }

    suspend fun fetchSitePlans(siteModel: SiteModel): FetchedPlansPayload {
        return if (siteModel.isUsingWpComRestApi) {
            coroutineEngine.withDefaultContext(T.API, this, "Fetch site plans") {
                return@withDefaultContext when (val response =
                    siteRestClient.fetchSitePlans(siteModel)) {
                    is Success -> {
                        FetchedPlansPayload(siteModel, response.data.plansList)
                    }
                    is Error -> {
                        val siteErrorType = when (response.error.apiError) {
                            "unauthorized" -> PlansErrorType.UNAUTHORIZED
                            "unknown_blog" -> PlansErrorType.UNKNOWN_BLOG
                            else -> PlansErrorType.GENERIC_ERROR
                        }
                        val plansError = PlansError(siteErrorType, response.error.message)
                        FetchedPlansPayload(siteModel, plansError)
                    }
                }
            }
        } else {
            FetchedPlansPayload(siteModel, PlansError(NOT_AVAILABLE))
        }
    }

    suspend fun fetchDomainPrice(domainName: String): WPAPIResponse<DomainPriceResponse> {
        return coroutineEngine.withDefaultContext(T.API, this, "Fetch domain price") {
            when (val response =
                siteRestClient.fetchDomainPrice(domainName)) {
                is Success -> {
                    WPAPIResponse.Success(response.data, response.headers)
                }
                is Error -> {
                    WPAPIResponse.Error(WPAPINetworkError(response.error))
                }
            }
        }
    }

    suspend fun launchSite(site: SiteModel): OnSiteLaunched {
        return coroutineEngine.withDefaultContext(T.API, this, "Launch site") {
            when (val response =
                siteRestClient.launchSite(site)) {
                is Success -> OnSiteLaunched()
                is Error -> {
                    val errorType = when (response.error.apiError) {
                        "unauthorized" -> LaunchSiteErrorType.UNAUTHORIZED
                        "already-launched" -> ALREADY_LAUNCHED
                        else -> LaunchSiteErrorType.GENERIC_ERROR
                    }
                    val error = LaunchSiteError(errorType, response.error.message)
                    OnSiteLaunched(error)
                }
            }
        }
    }

    private fun reportXmlrpcTry() {
        crashLogger.get().sendReport(
            null,
            emptyMap(),
            "Requested SiteStore XMLRPC connection. This should not happen."
        )
    }
}
