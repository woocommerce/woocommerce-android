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
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.error.ApplicationPasswordsDisabledDialogFragment
import com.woocommerce.android.ui.login.error.notwoo.LoginNotWooDialogFragment
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.LoggedIn
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowApplicationPasswordTutorialScreen
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowApplicationPasswordsUnavailableScreen
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowHelpScreen
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowNonWooErrorScreen
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowResetPasswordScreen
import com.woocommerce.android.ui.login.sitecredentials.applicationpassword.ApplicationPasswordTutorialFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginListener
import javax.inject.Inject

@AndroidEntryPoint
class LoginSiteCredentialsFragment : Fragment() {
    companion object {
        const val TAG = "LoginSiteCredentialsFragment"
        fun newInstance(siteAddress: String, isJetpackConnected: Boolean, username: String?, password: String?) =
            LoginSiteCredentialsFragment().apply {
                arguments = bundleOf(
                    LoginSiteCredentialsViewModel.SITE_ADDRESS_KEY to siteAddress,
                    LoginSiteCredentialsViewModel.IS_JETPACK_CONNECTED_KEY to isJetpackConnected,
                    LoginSiteCredentialsViewModel.USERNAME_KEY to username.orEmpty(),
                    LoginSiteCredentialsViewModel.PASSWORD_KEY to password.orEmpty()
                )
            }
    }

    private val viewModel: LoginSiteCredentialsViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private val loginListener: LoginListener
        get() = (requireActivity() as LoginListener)

    private val passwordTutorialListener: Listener?
        get() = (requireActivity() as? Listener)

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
        setupFragmentResultListeners()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is LoggedIn -> loginListener.loggedInViaUsernamePassword(arrayListOf(it.localSiteId))
                is ShowResetPasswordScreen -> loginListener.forgotPassword(it.siteAddress)
                is ShowNonWooErrorScreen -> LoginNotWooDialogFragment.newInstance(it.siteAddress)
                    .show(childFragmentManager, LoginNotWooDialogFragment.TAG)
                is ShowApplicationPasswordsUnavailableScreen ->
                    ApplicationPasswordsDisabledDialogFragment.newInstance(it.siteAddress, it.isJetpackConnected)
                        .show(childFragmentManager, LoginNotWooDialogFragment.TAG)
                is ShowHelpScreen -> loginListener.helpUsernamePassword(it.siteAddress, it.username, false)
                is ShowSnackbar -> uiMessageResolver.showSnack(it.message)
                is ShowApplicationPasswordTutorialScreen ->
                    passwordTutorialListener?.onApplicationPasswordHelpRequired(it.url, it.errorMessage)
                is ShowUiStringSnackbar -> uiMessageResolver.showSnack(it.message)
                is Exit -> requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupFragmentResultListeners() {
        childFragmentManager.setFragmentResultListener(
            LoginNotWooDialogFragment.INSTALLATION_RESULT,
            viewLifecycleOwner
        ) { _, _ ->
            viewModel.onWooInstallationAttempted()
        }

        childFragmentManager.setFragmentResultListener(
            ApplicationPasswordsDisabledDialogFragment.RETRY_RESULT,
            viewLifecycleOwner
        ) { _, _ ->
            viewModel.retryApplicationPasswordsCheck()
        }

        parentFragmentManager.setFragmentResultListener(
            ApplicationPasswordTutorialFragment.WEB_NAVIGATION_RESULT,
            viewLifecycleOwner
        ) { _, result ->
            result.getString(ApplicationPasswordTutorialFragment.URL_KEY)
                ?.takeIf { it.isNotEmpty() }
                ?.let { viewModel.onWebAuthorizationUrlLoaded(it) }
                ?: viewModel.onPasswordTutorialAborted()
        }
    }

    interface Listener {
        fun onApplicationPasswordHelpRequired(url: String, errorMessage: String)
    }
}
