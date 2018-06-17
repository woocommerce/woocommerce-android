package com.woocommerce.android.ui.login

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class LoginEpiloguePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore
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

    override fun getWooCommerceSites() = wooCommerceStore.getWooCommerceSites()

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (!event.isError) {
            loginEpilogueView?.showUserInfo()
            loginEpilogueView?.showSiteList()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (!event.isError) {
            loginEpilogueView?.showUserInfo()
            loginEpilogueView?.showSiteList()
        }
    }
}
