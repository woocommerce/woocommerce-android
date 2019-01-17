package com.woocommerce.android.ui.login

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
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.OnApiVersionFetched
import javax.inject.Inject

class LoginEpiloguePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val wooCommerceStore: WooCommerceStore
) : LoginEpilogueContract.Presenter {
    private var loginEpilogueView: LoginEpilogueContract.View? = null

    private var supportedWCSites = mutableListOf<SiteModel>()
    private var unsupportedWCSites = mutableListOf<SiteModel>()

    override fun takeView(view: LoginEpilogueContract.View) {
        dispatcher.register(this)
        loginEpilogueView = view
    }

    override fun dropView() {
        dispatcher.unregister(this)
        loginEpilogueView = null
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

    override fun checkWCVersionsForAllSites() {
        supportedWCSites.clear()
        unsupportedWCSites.clear()
        val wcSites = wooCommerceStore.getWooCommerceSites()
        if (wcSites.isEmpty()) {
            loginEpilogueView?.showStoreList(emptyList(), emptyList())
            return
        }

        for (site in wcSites) {
            dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteApiVersionAction(site))
        }
    }

    override fun getSitesForLocalIds(siteIdList: IntArray): List<SiteModel> {
        return siteIdList.map { siteStore.getSiteByLocalId(it) }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (!event.isError && !userIsLoggedIn()) {
            loginEpilogueView?.cancel()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiVersionFetched(event: OnApiVersionFetched) {
        if (event.isError) {
            WooLog.e(T.LOGIN, "Error fetching apiVersion for site [${event.site.siteId} : ${event.site.name}]! " +
                    "${event.error?.message}")
            loginEpilogueView?.errorVerifyingSites()
            return
        }

        // Check for empty API version as well (which may not result in an error from the api)
        if (event.apiVersion.isBlank()) {
            WooLog.e(T.LOGIN, "Empty apiVersion for site [${event.site.siteId} : ${event.site.name}]!")
            loginEpilogueView?.errorVerifyingSites()
            return
        }

        if (event.apiVersion == WooCommerceStore.WOO_API_NAMESPACE_V3) {
            supportedWCSites.add(event.site)
        } else {
            unsupportedWCSites.add(event.site)
        }

        val totalSitesChecked = supportedWCSites.size + unsupportedWCSites.size
        if (totalSitesChecked == wooCommerceStore.getWooCommerceSites().size) {
            loginEpilogueView?.showStoreList(supportedWCSites, unsupportedWCSites)
        }
    }
}
