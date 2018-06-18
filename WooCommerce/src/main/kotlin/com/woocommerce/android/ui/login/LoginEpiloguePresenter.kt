package com.woocommerce.android.ui.login

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class LoginEpiloguePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore
) : LoginEpilogueContract.Presenter {
    private var loginEpilogueView: LoginEpilogueContract.View? = null

    override fun takeView(view: LoginEpilogueContract.View) {
        dispatcher.register(this)
        loginEpilogueView = view
    }

    override fun dropView() {
        dispatcher.unregister(this)
        loginEpilogueView = null
    }

    override fun getWooCommerceSites() = wooCommerceStore.getWooCommerceSites()

    override fun logout() {
        dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
        dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())
    }

    override fun userIsLoggedIn(): Boolean {
        return accountStore.hasAccessToken()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (!event.isError && !userIsLoggedIn()) {
            loginEpilogueView?.cancel()
        }
    }
}
