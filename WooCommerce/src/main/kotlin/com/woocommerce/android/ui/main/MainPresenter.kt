package com.woocommerce.android.ui.main

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class MainPresenter @Inject constructor(
        private var dispatcher: Dispatcher,
        private var accountStore: AccountStore,
        private var siteStore: SiteStore,
        private var wooCommerceStore: WooCommerceStore
) : MainContract.Presenter {
    private var mainView: MainContract.View? = null

    override fun takeView(view: MainContract.View) {
        mainView = view
        dispatcher.register(this)

        if (userIsLoggedIn()) mainView?.updateStoreList(wooCommerceStore.getWooCommerceSites())
    }

    override fun dropView() {
        mainView = null
        dispatcher.unregister(this)
    }

    override fun userIsLoggedIn(): Boolean {
        return accountStore.hasAccessToken()
    }

    override fun storeMagicLinkToken(token: String) {
        // Save Token to the AccountStore. This will trigger an OnAuthenticationChanged.
        dispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(UpdateTokenPayload(token)))
    }

    override fun logout() {
        // Reset default account
        dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
        // Delete wpcom and jetpack sites
        dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAuthenticationChanged(event: OnAuthenticationChanged) {
        if (event.isError) {
            // TODO Handle AuthenticationErrorType.INVALID_TOKEN
            return
        }

        if (userIsLoggedIn()) {
            // This means a login via magic link was performed, and the access token was just updated
            // In this case, we need to fetch account details and the site list, and finally notify the view
            // In all other login cases, this logic is handled by the login library
            mainView?.notifyTokenUpdated()
            dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
        } else {
            mainView?.showLoginScreen()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            // TODO: Notify the user of the problem
            return
        }

        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
            // The user's account info has been fetched and stored - next, fetch the user's settings
            dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            // The user's account settings have also been fetched and stored - now we can fetch the user's sites
            dispatcher.dispatch(SiteActionBuilder.newFetchSitesAction())
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            // TODO: Notify the user of the problem
            return
        }

        mainView?.updateStoreList(wooCommerceStore.getWooCommerceSites())
    }
}
