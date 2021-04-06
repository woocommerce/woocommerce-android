package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
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
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.OnApiVersionFetched
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class SitePickerPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val wooCommerceStore: WooCommerceStore
) : SitePickerContract.Presenter {
    private var view: SitePickerContract.View? = null

    override fun takeView(view: SitePickerContract.View) {
        dispatcher.register(this)
        this.view = view
    }

    override fun dropView() {
        dispatcher.unregister(this)
        view = null
    }

    override fun getWooCommerceSites() = wooCommerceStore.getWooCommerceSites()

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
        val wcSites = wooCommerceStore.getWooCommerceSites()
        if (wcSites.size > 0) {
            view?.showStoreList(wcSites)
        } else {
            view?.showSkeleton(true)
        }
        fetchSitesFromAPI()
    }

    override fun fetchSitesFromAPI() {
        dispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(FetchSitesPayload()))
    }

    override fun fetchUpdatedSiteFromAPI(site: SiteModel) {
        view?.showSkeleton(true)
        dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site))
    }

    override fun loadSites() {
        val wcSites = wooCommerceStore.getWooCommerceSites()
        view?.showStoreList(wcSites)
    }

    override fun verifySiteApiVersion(site: SiteModel) {
        dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteApiVersionAction(site))
    }

    override fun updateWooSiteSettings(site: SiteModel) {
        dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteSettingsAction(site))
    }

    override fun getSitesForLocalIds(siteIdList: IntArray): List<SiteModel> {
        return siteIdList.map { siteStore.getSiteByLocalId(it) }
    }

    override fun getSiteModelByUrl(url: String): SiteModel? =
            SiteUtils.getSiteByMatchingUrl(siteStore, url)

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            WooLog.e(T.LOGIN, "Account error [type = ${event.causeOfChange}] : " +
                    "${event.error.type} > ${event.error.message}")
        } else if (!userIsLoggedIn()) {
            view?.didLogout()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        view?.showSkeleton(false)
        if (event.isError) {
            WooLog.e(T.LOGIN, "Site error [${event.error.type}] : ${event.error.message}")
        } else {
            loadSites()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiVersionFetched(event: OnApiVersionFetched) {
        if (event.isError) {
            WooLog.e(T.LOGIN, "Error fetching apiVersion for site [${event.site.siteId} : ${event.site.name}]! " +
                    "${event.error?.type} - ${event.error?.message}")
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
