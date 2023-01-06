package com.woocommerce.android.ui.login.error

import androidx.core.os.bundleOf
import com.woocommerce.android.R
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.error.base.LoginBaseErrorDialogFragment

class LoginNonWooFragment : LoginBaseErrorDialogFragment() {
    companion object {
        const val TAG = "LoginNonWooFragment"
        private const val SITE_URL_KEY = "site-url"
        fun newInstance(siteUrl: String) = LoginNonWooFragment().apply {
            arguments = bundleOf(SITE_URL_KEY to siteUrl)
        }
    }

    override val text: String
        get() = getString(R.string.login_not_woo_store, requireArguments().getString(SITE_URL_KEY))

    override val helpOrigin: HelpOrigin
        get() = HelpOrigin.LOGIN_SITE_ADDRESS
}
