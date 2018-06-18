package com.woocommerce.android.ui.login

import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class LoginEpiloguePresenter @Inject constructor(
    private val wooCommerceStore: WooCommerceStore
) : LoginEpilogueContract.Presenter {
    private var loginEpilogueView: LoginEpilogueContract.View? = null

    override fun takeView(view: LoginEpilogueContract.View) {
        loginEpilogueView = view
    }

    override fun dropView() {
        loginEpilogueView = null
    }

    override fun getWooCommerceSites() = wooCommerceStore.getWooCommerceSites()
}
