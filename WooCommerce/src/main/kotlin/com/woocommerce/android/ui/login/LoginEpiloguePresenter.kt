package com.woocommerce.android.ui.login

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class LoginEpiloguePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountstore: AccountStore,
    private val siteStore: SiteStore
) : LoginEpilogueContract.Presenter {
    private var loginEpilogueView: LoginEpilogueContract.View? = null

    override fun takeView(view: LoginEpilogueContract.View) {
        loginEpilogueView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        loginEpilogueView = null
        dispatcher.unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            // TODO: Notify the user of the problem
            return
        }

        loginEpilogueView?.updateAvatar()
    }
}
