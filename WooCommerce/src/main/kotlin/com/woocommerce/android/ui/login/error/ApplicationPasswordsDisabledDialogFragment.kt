package com.woocommerce.android.ui.login.error

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.woocommerce.android.R
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.error.base.LoginBaseErrorDialogFragment
import com.woocommerce.android.util.ChromeCustomTabUtils

class ApplicationPasswordsDisabledDialogFragment : LoginBaseErrorDialogFragment() {
    companion object {
        const val RETRY_RESULT = "retry"
        private const val SITE_URL_KEY = "site-url"
        private const val IS_JETPACK_CONNECTED_KEY = "is-jetpack-connected"
        private const val APPLICATION_PASSWORDS_GUIDE =
            "https://make.wordpress.org/core/2020/11/05/application-passwords-integration-guide/"

        fun newInstance(siteUrl: String, isJetpackConnected: Boolean) =
            ApplicationPasswordsDisabledDialogFragment().apply {
                arguments = bundleOf(
                    SITE_URL_KEY to siteUrl,
                    IS_JETPACK_CONNECTED_KEY to isJetpackConnected
                )
            }
    }

    private val isJetpackConnected by lazy { requireArguments().getBoolean(IS_JETPACK_CONNECTED_KEY) }

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

    override val primaryButton: LoginErrorButton
        get() = if (isJetpackConnected) {
            LoginErrorButton(
                title = R.string.login_with_wordpress,
                onClick = {
                    dismiss()
                    (requireActivity() as LoginActivity).showEmailLoginScreen()
                }
            )
        } else {
            LoginErrorButton(
                title = R.string.retry,
                onClick = {
                    setFragmentResult(RETRY_RESULT, bundleOf())
                    dismiss()
                }
            )
        }
}
