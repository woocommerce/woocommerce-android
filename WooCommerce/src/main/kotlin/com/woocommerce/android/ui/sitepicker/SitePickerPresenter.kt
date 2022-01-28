package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.WCCoreActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.OnApiVersionFetched
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class SitePickerPresenter
@Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val wooCommerceStore: WooCommerceStore,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : SitePickerContract.Presenter {
    private var view: SitePickerContract.View? = null

    private var hasFetchedWooSites = false

    override fun takeView(view: SitePickerContract.View) {
        dispatcher.register(this)
        this.view = view
    }

    override fun dropView() {
        dispatcher.unregister(this)
        view = null
    }

    override fun getWooCommerceSites(): List<SiteModel> {
        val currentWooSites = wooCommerceStore.getWooCommerceSites()
        return if (!FeatureFlag.JETPACK_CP.isEnabled() ||
            hasFetchedWooSites ||
            currentWooSites.none { it.isJetpackCPConnected }
        ) {
            currentWooSites
        } else {
            emptyList()
        }
    }

    override fun getSiteBySiteId(siteId: Long): SiteModel? = siteStore.getSiteBySiteId(siteId)

    override fun getUserAvatarUrl() = accountStore.account?.avatarUrl

    override fun getUserName() = accountStore.account?.userName

    override fun getUserDisplayName() = accountStore.account?.displayName

    override fun logout() {
        dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
        dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())
    }

    override fun userIsLoggedIn(): Boolean {
        return accountStore.hasAccessToken()
    }

    override fun loadAndFetchSites() {
        val wcSites = getWooCommerceSites()
        if (wcSites.isNotEmpty()) {
            view?.showStoreList(wcSites)
        } else {
            view?.showLoadingView(true)
        }
        fetchSitesFromAPI()
    }

    override fun fetchSitesFromAPI() {
        coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) { wooCommerceStore.fetchWooCommerceSites() }
            val duration = System.currentTimeMillis() - startTime
            view?.showLoadingView(false)
            if (result.isError) {
                WooLog.e(T.LOGIN, "Site error [${result.error.type}] : ${result.error.message}")
            } else {
                hasFetchedWooSites = true
                view?.showStoreList(result.model!!)
                if (result.model?.any { it.isJetpackCPConnected } == true) {
                    AnalyticsTracker.track(
                        stat = Stat.JETPACK_CP_SITES_FETCHED,
                        properties = mapOf(AnalyticsTracker.KEY_FETCH_SITES_DURATION to duration)
                    )
                }
            }
        }
    }

    override fun fetchUpdatedSiteFromAPI(site: SiteModel) {
        coroutineScope.launch {
            view?.showLoadingView(true)
            val result = wooCommerceStore.fetchWooCommerceSite(site)
            view?.showLoadingView(false)
            if (result.isError) {
                WooLog.e(T.LOGIN, "Site error [${result.error.type}] : ${result.error.message}")
            }
            loadSites()
        }
    }

    override fun fetchUserRoleFromAPI(site: SiteModel) {
        coroutineScope.launch {
            val userModel = userEligibilityFetcher.fetchUserInfo()
            view?.hideProgressDialog()

            userModel?.let {
                userEligibilityFetcher.updateUserInfo(it)
                withContext(Dispatchers.Main) {
                    view?.userVerificationCompleted()
                }
            }
        }
    }

    override fun loadSites() {
        val wcSites = getWooCommerceSites()
        view?.showStoreList(wcSites)
    }

    override fun verifySiteApiVersion(site: SiteModel) {
        dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteApiVersionAction(site))
    }

    override fun updateWooSiteSettings(site: SiteModel) {
        dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteSettingsAction(site))
    }

    override fun getSitesForLocalIds(siteIdList: IntArray): List<SiteModel> {
        return siteIdList.toList().mapNotNull { siteStore.getSiteByLocalId(it) }
    }

    override fun getSiteModelByUrl(url: String): SiteModel? =
        SiteUtils.getSiteByMatchingUrl(siteStore, url)?.takeIf {
            FeatureFlag.JETPACK_CP.isEnabled() || !it.isJetpackCPConnected
        }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            WooLog.e(
                T.LOGIN,
                "Account error [type = ${event.causeOfChange}] : " +
                    "${event.error.type} > ${event.error.message}"
            )
        } else if (!userIsLoggedIn()) {
            view?.didLogout()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiVersionFetched(event: OnApiVersionFetched) {
        if (event.isError) {
            WooLog.e(
                T.LOGIN,
                "Error fetching apiVersion for site [${event.site.siteId} : ${event.site.name}]! " +
                    "${event.error?.type} - ${event.error?.message}"
            )
            view?.siteVerificationError(event.site)
            return
        }

        // Check for empty API version as well (which may not result in an error from the api)
        if (event.apiVersion.isBlank()) {
            WooLog.e(T.LOGIN, "Empty apiVersion for site [${event.site.siteId} : ${event.site.name}]!")
            view?.siteVerificationError(event.site)
            return
        }

        if (event.apiVersion == WooCommerceStore.WOO_API_NAMESPACE_V3) {
            view?.siteVerificationPassed(event.site)
        } else {
            view?.siteVerificationFailed(event.site)
        }
    }
}
