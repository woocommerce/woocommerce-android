package com.woocommerce.android.ui.login.sitecredentials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginSiteCredentialsFragment : Fragment() {
    companion object {
        const val TAG = "LoginSiteCredentialsFragment"
        fun newInstance(siteAddress: String, username: String?, password: String?) =
            LoginSiteCredentialsFragment().apply {
                arguments = bundleOf(
                    LoginSiteCredentialsViewModel.SITE_ADDRESS_KEY to siteAddress,
                    LoginSiteCredentialsViewModel.USERNAME_KEY to username,
                    LoginSiteCredentialsViewModel.PASSWORD_KEY to password
                )
            }
    }

    private val viewModel: LoginSiteCredentialsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                LoginSiteCredentialsScreen(viewModel = viewModel)
            }
        }
    }
}
