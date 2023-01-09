package com.woocommerce.android.ui.login.error

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.woocommerce.android.R
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.error.base.LoginBaseErrorDialogFragment
import com.woocommerce.android.util.ChromeCustomTabUtils

class ApplicationPasswordsDisabledDialogFragment : LoginBaseErrorDialogFragment() {
    companion object {
        private const val SITE_URL_KEY = "site-url"
        private const val APPLICATION_PASSWORDS_GUIDE =
            "https://make.wordpress.org/core/2020/11/05/application-passwords-integration-guide/"

        fun newInstance(siteUrl: String) = ApplicationPasswordsDisabledDialogFragment().apply {
            arguments = bundleOf(SITE_URL_KEY to siteUrl)
        }
    }

    override val text: CharSequence
        get() = getString(R.string.login_application_passwords_unavailable, requireArguments().getString(SITE_URL_KEY))
    override val helpOrigin: HelpOrigin
        get() = HelpOrigin.LOGIN_SITE_ADDRESS

    override val inlineButtons: List<LoginErrorButton>
        get() = listOf(
            LoginErrorButton(
                title = R.string.login_application_passwords_help,
                onClick = {
                    ChromeCustomTabUtils.launchUrl(requireContext(), APPLICATION_PASSWORDS_GUIDE)
                }
            )
        )
}
