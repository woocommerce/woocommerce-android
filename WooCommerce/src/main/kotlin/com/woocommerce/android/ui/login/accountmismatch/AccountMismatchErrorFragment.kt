package com.woocommerce.android.ui.login.accountmismatch

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginEmailHelpDialogFragment
import com.woocommerce.android.ui.login.LoginEmailHelpDialogFragment.Listener
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.NavigateToEmailHelpDialogEvent
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.NavigateToHelpScreen
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.NavigateToLoginScreen
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.NavigateToSiteAddressEvent
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.OnJetpackConnectedEvent
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.ViewState
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginMode
import javax.inject.Inject

@AndroidEntryPoint
class AccountMismatchErrorFragment : BaseFragment(), Listener {
    private val viewModel: AccountMismatchErrorViewModel by viewModels()
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val backButtonCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // The callback is enabled only when showing the WebView
            (viewModel.viewState.value as? ViewState.JetpackWebViewState)?.onDismiss?.invoke()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    AccountMismatchErrorScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backButtonCallback)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewState.observe(viewLifecycleOwner) {
            backButtonCallback.isEnabled = it is ViewState.JetpackWebViewState
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToHelpScreen -> {
                    startActivity(
                        HelpActivity.createIntent(
                            context = requireContext(),
                            origin = HelpActivity.Origin.LOGIN_SITE_ADDRESS,
                            extraSupportTags = null
                        )
                    )
                }
                is NavigateToEmailHelpDialogEvent -> {
                    LoginEmailHelpDialogFragment.newInstance(this).also {
                        it.show(parentFragmentManager, LoginEmailHelpDialogFragment.TAG)
                    }
                }
                is NavigateToSiteAddressEvent -> findNavController().navigateSafely(
                    AccountMismatchErrorFragmentDirections
                        .actionAccountMismatchErrorFragmentToSitePickerSiteDiscoveryFragment()
                )
                is NavigateToLoginScreen -> navigateToLoginScreen()
                is OnJetpackConnectedEvent -> onJetpackConnected(event.email)
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun onJetpackConnected(email: String) {
        if (requireActivity() is LoginListener) {
            // Make sure this fragment is removed from the backstack
            requireActivity().supportFragmentManager.popBackStack()
            (requireActivity() as LoginListener).gotWpcomEmail(email, true, null)
        }
    }

    private fun navigateToLoginScreen() {
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            LoginMode.WOO_LOGIN_MODE.putInto(this)
        }
        startActivity(intent)
    }

    override fun onEmailNeedMoreHelpClicked() {
        startActivity(
            HelpActivity.createIntent(
                context = requireContext(),
                origin = HelpActivity.Origin.LOGIN_CONNECTED_EMAIL_HELP,
                extraSupportTags = null
            )
        )
    }
}
