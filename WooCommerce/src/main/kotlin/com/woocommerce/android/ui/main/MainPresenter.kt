package com.woocommerce.android.ui.main

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import javax.inject.Inject

class MainPresenter @Inject constructor(
        private var dispatcher: Dispatcher,
        private var accountStore: AccountStore
) : MainContract.Presenter {
    private var mainView: MainContract.View? = null

    override fun takeView(view: MainContract.View) {
        mainView = view
        dispatcher.register(this)
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAuthenticationChanged(event: OnAuthenticationChanged) {
        if (event.isError) {
            // TODO Handle AuthenticationErrorType.INVALID_TOKEN
            return
        }

        if (userIsLoggedIn()) {
            mainView?.notifyTokenUpdated()
        }
    }
}
