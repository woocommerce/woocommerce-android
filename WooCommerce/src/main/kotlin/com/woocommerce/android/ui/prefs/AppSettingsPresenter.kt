package com.woocommerce.android.ui.prefs

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import javax.inject.Inject

class AppSettingsPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore
) : AppSettingsContract.Presenter {
    private var appSettingsView: AppSettingsContract.View? = null

    override fun takeView(view: AppSettingsContract.View) {
        dispatcher.register(this)
        appSettingsView = view
    }

    override fun dropView() {
        dispatcher.unregister(this)
        appSettingsView = null
    }

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
            appSettingsView?.close()
        }
    }
}
