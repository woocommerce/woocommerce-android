package com.woocommerce.android.ui.login.overrides

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import org.wordpress.android.login.LoginSiteAddressFragment

class WooLoginSiteAddressFragment : LoginSiteAddressFragment() {
    @LayoutRes
    override fun getContentLayout() = R.layout.fragment_login_site_address

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
    }
}
