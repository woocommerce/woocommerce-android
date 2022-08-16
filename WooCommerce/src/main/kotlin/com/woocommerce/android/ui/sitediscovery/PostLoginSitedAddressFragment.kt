package com.woocommerce.android.ui.sitediscovery

import org.wordpress.android.login.LoginSiteAddressFragment

class PostLoginSitedAddressFragment : LoginSiteAddressFragment() {
    override fun handleWpComDiscoveryError(failedEndpoint: String?) {
        // Discovery fails for a WPCom site happens for non-atomic cases
        mLoginListener.gotWpcomSiteInfo(failedEndpoint)
    }
}
