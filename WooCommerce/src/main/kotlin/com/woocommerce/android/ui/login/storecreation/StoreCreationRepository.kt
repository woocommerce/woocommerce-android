package com.woocommerce.android.ui.login.storecreation

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Parcelable
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.FREE_TRIAL_ASSIGNMENT_FAILED
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.PLAN_PURCHASE_FAILED
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_CREATION_FAILED
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_LOADING_FAILED
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Failure
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Success
import com.woocommerce.android.ui.login.storecreation.plans.BillingPeriod
import com.woocommerce.android.ui.plans.networking.SitePlanRestClient
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.LOGIN
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plans.full.Plan
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.ShoppingCart.CartProduct
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartStore
import org.wordpress.android.fluxc.store.PlansStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType.SITE_NAME_EXISTS
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.fluxc.store.SiteStore.SiteFilter.WPCOM
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.COMING_SOON
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.login.util.SiteUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class StoreCreationRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val siteStore: SiteStore,
    private val shoppingCartStore: ShoppingCartStore,
    private val dispatcher: Dispatcher,
    private val plansStore: PlansStore,
    private val userEligibilityFetcher: UserEligibilityFetcher,
    private val sitePlanRestClient: SitePlanRestClient,
    private val newStore: NewStore
) {

    companion object {
        private const val FREE_TRIAL_SITE_CREATION_FLOW = "wooexpress"
    }

    fun selectSite(site: SiteModel) {
        selectedSite.set(site)
    }

    suspend fun selectSite(siteId: Long) {
        siteStore.getSiteBySiteId(siteId)?.let {
            userEligibilityFetcher.fetchUserInfo(it) // prefetch user info so it's available when the store is loaded
            selectedSite.set(it)
        }
    }

    suspend fun fetchSitesAfterCreation(): Result<List<SiteModel>> {
        val result = withContext(Dispatchers.Default) {
            wooCommerceStore.fetchWooCommerceSites()
        }
        return if (result.isError) Result.failure(Exception(result.error.message))
        else Result.success(result.model ?: emptyList())
    }

    suspend fun getSiteByUrl(url: String?): SiteModel? {
        siteStore.fetchSites(FetchSitesPayload(listOf(WPCOM)))
        return url?.let {
            siteStore.getSitesByNameOrUrlMatching(url).firstOrNull()
        }
    }

    suspend fun fetchSite(siteId: Long): StoreCreationResult<Unit> {
        val result = withContext(Dispatchers.Default) {
            val site = SiteModel().apply {
                this.siteId = siteId
                this.origin = SiteModel.ORIGIN_WPCOM_REST
                this.setIsWPCom(true)
            }

            siteStore.fetchSite(site)
        }
        return when {
            result.isError -> Failure(STORE_LOADING_FAILED, result.error?.message)
            else -> Success(Unit)
        }
    }

    suspend fun fetchPlan(period: BillingPeriod): Plan? {
        val fetchResult = plansStore.fetchPlans()
        return when {
            fetchResult.isError -> {
                WooLog.e(LOGIN, "Error fetching plans: ${fetchResult.error.message}")
                null
            }

            fetchResult.plans == null -> {
                WooLog.e(LOGIN, "Error fetching plans: null response")
                null
            }

            else -> {
                fetchResult.plans!!.firstOrNull { it.productSlug == period.slug }
            }
        }
    }

    fun getSiteBySiteUrl(url: String) = SiteUtils.getSiteByMatchingUrl(siteStore, url).takeIf {
        // Take only sites returned from the WPCom /me/sites response
        it?.origin == SiteModel.ORIGIN_WPCOM_REST
    }

    suspend fun addPlanToCart(
        planProductId: Int?,
        planPathSlug: String?,
        siteId: Long?
    ): StoreCreationResult<Unit> {
        if (planProductId == null || planPathSlug == null || siteId == null) {
            WooLog.e(
                tag = LOGIN,
                message = "Missing plan purchase data: productId=$planProductId, pathSlug=$planPathSlug, siteId=$siteId"
            )
            return Failure(PLAN_PURCHASE_FAILED)
        } else {
            val eCommerceProduct = CartProduct(
                productId = planProductId,
                extra = mapOf(
                    "context" to "signup",
                    "signup_flow" to planPathSlug
                )
            )

            shoppingCartStore.addProductToCart(siteId, eCommerceProduct).let { result ->
                return when {
                    result.isError -> {
                        WooLog.e(
                            LOGIN,
                            "Error adding eCommerce plan to cart: ${result.error.message}"
                        )
                        Failure(PLAN_PURCHASE_FAILED, result.error.message)
                    }

                    result.model != null -> Success(Unit)
                    else -> Failure(PLAN_PURCHASE_FAILED, "Null response")
                }
            }
        }
    }

    suspend fun createNewFreeTrialSite(
        siteData: SiteCreationData,
        languageWordPressId: String,
        timeZoneId: String,
    ): StoreCreationResult<Long> {
        val createSiteResult = createNewSite(
            siteData,
            languageWordPressId,
            timeZoneId,
            COMING_SOON,
            siteCreationFlow = FREE_TRIAL_SITE_CREATION_FLOW,
            shouldFindAvailableUrl = true
        )

        return when (createSiteResult) {
            is Success -> {
                sitePlanRestClient.addEcommercePlanTrial(createSiteResult.data)
                    .let { addTrialResult ->
                        return when (addTrialResult) {
                            is WPComGsonRequestBuilder.Response.Success -> {
                                Success(createSiteResult.data)
                            }

                            is WPComGsonRequestBuilder.Response.Error -> {
                                Failure(FREE_TRIAL_ASSIGNMENT_FAILED)
                            }
                        }
                    }
            }

            is Failure -> createSiteResult
        }
    }

    suspend fun createNewSite(
        siteData: SiteCreationData,
        languageWordPressId: String,
        timeZoneId: String,
        siteVisibility: SiteVisibility = COMING_SOON,
        dryRun: Boolean = false,
        siteCreationFlow: String? = null,
        shouldFindAvailableUrl: Boolean? = null,
    ): StoreCreationResult<Long> {
        fun isWordPressComSubDomain(url: String) = url.endsWith(".wordpress.com")

        fun extractSubDomain(domain: String): String {
            val str = UrlUtils.addUrlSchemeIfNeeded(domain, false)
            val host = UrlUtils.getHost(str)
            if (host.isNotEmpty()) {
                val parts = host.split(".").toTypedArray()
                if (parts.size > 1) { // There should be at least 2 dots for there to be a subdomain.
                    return parts[0]
                }
            }
            return ""
        }

        val domain = when {
            siteData.domain.isNullOrEmpty() -> null
            isWordPressComSubDomain(siteData.domain) -> extractSubDomain(siteData.domain)
            else -> siteData.domain
        }
        val newSitePayload = NewSitePayload(
            siteName = domain,
            siteTitle = siteData.title,
            language = languageWordPressId,
            timeZoneId = timeZoneId,
            visibility = siteVisibility,
            segmentId = siteData.segmentId,
            dryRun = dryRun,
            siteCreationFlow = siteCreationFlow,
            findAvailableUrl = shouldFindAvailableUrl
        )

        val result = dispatcher.dispatchAndAwait<NewSitePayload, OnNewSiteCreated>(
            SiteActionBuilder.newCreateNewSiteAction(newSitePayload)
        )

        return when {
            result.isError -> {
                if (result.error.type == SITE_NAME_EXISTS) {
                    Failure(SITE_ADDRESS_ALREADY_EXISTS, result.error.message)
                } else {
                    WooLog.e(LOGIN, "${result.error.type}: ${result.error.message}")
                    Failure(SITE_CREATION_FAILED, result.error.message)
                }
            }

            else -> {
                newStore.update(domain = Uri.parse(result.url.orEmpty()).host.orEmpty())
                Success(result.newSiteRemoteId)
            }
        }
    }

    @Parcelize
    @SuppressLint("ParcelCreator")
    data class SiteCreationData(
        val segmentId: Long?,
        val domain: String?,
        val title: String?,
        val profilerData: NewStore.ProfilerData? = null,
        val countryCode: String? = null,
    ) : Parcelable
}
