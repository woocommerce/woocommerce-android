package com.woocommerce.android.ui.login.storecreation

import android.annotation.SuppressLint
import android.os.Parcelable
import com.woocommerce.android.tools.SelectedSite
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
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartStore
import org.wordpress.android.fluxc.store.PlansStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.PUBLIC
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
    private val plansStore: PlansStore
) {
    companion object {
        private const val ECOMMMERCE_MONTHLY_PLAN_SLUG = "ecommerce-bundle-monthly"
    }

    fun selectSite(site: SiteModel) {
        selectedSite.set(site)
    }

    fun selectSite(siteId: Long) {
        siteStore.getSiteBySiteId(siteId)?.let {
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

    suspend fun fetchSiteAfterCreation(siteId: Long): Result<Boolean> {
        val result = withContext(Dispatchers.Default) {
            val site = SiteModel().apply {
                this.siteId = siteId
                this.setIsWPCom(true)
            }
            siteStore.fetchSite(site)
        }
        return if (result.isError) {
            Result.failure(Exception(result.error.message))
        } else {
            val isSiteReady = siteStore.getSiteBySiteId(siteId)?.isJetpackConnected ?: false
            Result.success(isSiteReady)
        }
    }

    suspend fun fetchWooPlan(): Plan? {
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
                fetchResult.plans!!.firstOrNull { it.productSlug == ECOMMMERCE_MONTHLY_PLAN_SLUG }
            }
        }
    }

    fun getSiteBySiteUrl(url: String) = SiteUtils.getSiteByMatchingUrl(siteStore, url).takeIf {
        // Take only sites returned from the WPCom /me/sites response
        it?.origin == SiteModel.ORIGIN_WPCOM_REST
    }

    suspend fun addPlanToCart(siteId: Long): WooResult<Unit> {
        shoppingCartStore.addWooCommercePlanToCart(siteId).let { result ->
            return when {
                result.isError -> {
                    WooLog.e(LOGIN, "Error adding eCommerce plan to cart: ${result.error.message}")
                    WooResult(result.error)
                }
                result.model != null -> WooResult(Unit)
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun createNewSite(
        siteData: SiteCreationData,
        languageWordPressId: String,
        timeZoneId: String,
        siteVisibility: SiteVisibility = PUBLIC,
        dryRun: Boolean = false
    ): WooResult<Long> {
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
            domain,
            siteData.title,
            languageWordPressId,
            timeZoneId,
            siteVisibility,
            siteData.segmentId,
            siteData.siteDesign,
            dryRun
        )

        val result = dispatcher.dispatchAndAwait<NewSitePayload, OnNewSiteCreated>(
            SiteActionBuilder.newCreateNewSiteAction(newSitePayload)
        )

        return when {
            result.isError -> {
                WooLog.e(LOGIN, "${result.error.type}: ${result.error.message}")
                WooResult(WooError(GENERIC_ERROR, UNKNOWN, result.error.message))
            }
            else -> WooResult(result.newSiteRemoteId)
        }
    }

    @Parcelize
    @SuppressLint("ParcelCreator")
    data class SiteCreationData(
        val segmentId: Long?,
        val siteDesign: String?,
        val domain: String?,
        val title: String?
    ) : Parcelable
}
