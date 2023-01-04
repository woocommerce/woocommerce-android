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
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.LoggedIn
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowResetPasswordScreen
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginListener

@AndroidEntryPoint
class LoginSiteCredentialsFragment : Fragment() {
    companion object {
        const val TAG = "LoginSiteCredentialsFragment"
        fun newInstance(siteAddress: String, username: String?, password: String?) =
            LoginSiteCredentialsFragment().apply {
                arguments = bundleOf(
                    LoginSiteCredentialsViewModel.SITE_ADDRESS_KEY to siteAddress,
                    LoginSiteCredentialsViewModel.USERNAME_KEY to username.orEmpty(),
                    LoginSiteCredentialsViewModel.PASSWORD_KEY to password.orEmpty()
                )
            }
    }

    private val viewModel: LoginSiteCredentialsViewModel by viewModels()

    private val loginListener: LoginListener
        get() = (requireActivity() as LoginListener)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    LoginSiteCredentialsScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is LoggedIn -> loginListener.loggedInViaUsernamePassword(arrayListOf(it.localSiteId))
                is ShowResetPasswordScreen -> loginListener.forgotPassword(it.siteAddress)
                is Exit -> requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
