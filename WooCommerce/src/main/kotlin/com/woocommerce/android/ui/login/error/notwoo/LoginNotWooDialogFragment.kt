package com.woocommerce.android.ui.login.error.notwoo

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.error.base.LoginBaseErrorDialogFragment
import com.woocommerce.android.ui.login.error.notwoo.LoginNotWooViewModel.LaunchWooInstallation
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult

class LoginNotWooDialogFragment : LoginBaseErrorDialogFragment() {
    companion object {
        const val TAG = "LoginNonWooFragment"
        const val SITE_URL_KEY = "site-url"
        const val INSTALLATION_RESULT = "installation-result"
        fun newInstance(siteUrl: String) = LoginNotWooDialogFragment().apply {
            arguments = bundleOf(SITE_URL_KEY to siteUrl)
        }
    }

    private val viewModel: LoginNotWooViewModel by viewModels()

    override val text: String
        get() = getString(R.string.login_not_woo_store, requireArguments().getString(SITE_URL_KEY))

    override val inlineButtons: List<LoginErrorButton>
        get() = listOf(
            LoginErrorButton(
                title = R.string.login_open_installation_page,
                onClick = viewModel::openWooInstallationScreen
            )
        )

    override val helpOrigin: HelpOrigin
        get() = HelpOrigin.LOGIN_SITE_ADDRESS

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(this) {
            when (it) {
                is LaunchWooInstallation -> ChromeCustomTabUtils.launchUrl(requireContext(), it.installationUrl)
                is ExitWithResult<*> -> {
                    setFragmentResult(INSTALLATION_RESULT, bundleOf())
                    dismiss()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
