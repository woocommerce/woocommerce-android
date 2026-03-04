package com.woocommerce.android.ui.login.wpcom

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.woocommerce.android.NavGraphJetpackActivationDirections
import com.woocommerce.android.NavGraphJetpackInstallDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.GoToStore
import com.woocommerce.android.ui.login.wpcom.WPComLoginPasswordViewModel.Show2FAScreen
import com.woocommerce.android.ui.login.wpcom.WPComLoginPasswordViewModel.ShowMagicLinkScreen
import com.woocommerce.android.ui.login.wpcom.WPComLoginPostLoginViewModel.ShowJetpackActivationScreen
import com.woocommerce.android.ui.login.wpcom.WPComLoginPostLoginViewModel.ShowJetpackCPInstallationScreen
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.LaunchUrlInChromeTab
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WPComLoginPasswordFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: WPComLoginPasswordViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    WPComLoginPasswordScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Show2FAScreen -> navigateTo2FAScreen(event)
                is ShowMagicLinkScreen -> navigateToMagicLinkScreen(event)
                is ShowJetpackActivationScreen -> navigateToJetpackActivationScreen(event)
                is ShowJetpackCPInstallationScreen -> navigateToJetpackCPInstallationScreen()
                is GoToStore -> goToStore()
                is LaunchUrlInChromeTab -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun navigateTo2FAScreen(event: Show2FAScreen) {
        findNavController().navigateSafely(
            WPComLoginPasswordFragmentDirections
                .actionWPComLoginPasswordFragmentToWPComLogin2FAFragment(
                    jetpackStatus = event.jetpackStatus,
                    emailOrUsername = event.emailOrUsername,
                    password = event.password,
                    userId = event.userId,
                    webauthnNonce = event.webauthnNonce,
                    supportedAuthTypes = event.supportedAuthTypes.toTypedArray()
                )
        )
    }

    private fun navigateToJetpackActivationScreen(event: ShowJetpackActivationScreen) {
        findNavController().navigateSafely(
            NavGraphJetpackActivationDirections.actionGlobalToJetpackActivationMainFragment(
                siteUrl = event.siteUrl,
                jetpackStatus = event.jetpackStatus
            )
        )
    }

    private fun navigateToMagicLinkScreen(event: ShowMagicLinkScreen) {
        findNavController().navigateSafely(
            WPComLoginPasswordFragmentDirections
                .actionWPComLoginPasswordFragmentToWPComLoginMagicLinkRequestFragment(
                    emailOrUsername = event.emailOrUsername,
                    jetpackStatus = event.jetpackStatus,
                    fallbackButton = event.magicLinkFallbackButton,
                    requestAtStart = event.requestAtStart,
                    isNewWpComAccount = false
                )
        )
    }

    private fun navigateToJetpackCPInstallationScreen() {
        findNavController().navigateSafely(
            NavGraphJetpackInstallDirections.actionGlobalJetpackCPInstallProgressDialog(),
            navOptions = navOptions {
                popUpTo(R.id.jetpackActivationDispatcherFragment) { inclusive = true }
            }
        )
    }

    private fun goToStore() {
        (requireActivity() as? MainActivity)?.handleSitePickerResult() ?: run {
            val intent = Intent(requireActivity(), MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
    }
}
