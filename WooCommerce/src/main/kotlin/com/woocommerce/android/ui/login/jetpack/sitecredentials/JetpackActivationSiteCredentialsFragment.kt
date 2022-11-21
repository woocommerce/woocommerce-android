package com.woocommerce.android.ui.login.jetpack.sitecredentials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.sitecredentials.JetpackActivationSiteCredentialsViewModel.NavigateToJetpackActivationSteps
import com.woocommerce.android.ui.login.jetpack.sitecredentials.JetpackActivationSiteCredentialsViewModel.ResetPassword
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class JetpackActivationSiteCredentialsFragment : BaseFragment() {
    companion object {
        private const val FORGOT_PASSWORD_URL_SUFFIX = "wp-login.php?action=lostpassword"
    }

    private val viewModel: JetpackActivationSiteCredentialsViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    JetpackActivationSiteCredentialsScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToJetpackActivationSteps -> navigateToJetpackActivationSteps(event)
                is ResetPassword -> showResetPasswordWebPage(event.siteUrl)
                Exit -> findNavController().navigateUp()
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowUiStringSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun navigateToJetpackActivationSteps(event: NavigateToJetpackActivationSteps) {
        findNavController().navigateSafely(
            JetpackActivationSiteCredentialsFragmentDirections
                .actionJetpackActivationSiteCredentialsFragmentToJetpackActivationMainFragment(
                    siteUrl = event.siteUrl,
                    isJetpackInstalled = event.isJetpackInstalled
                )
        )
    }

    private fun showResetPasswordWebPage(siteUrl: String) {
        ChromeCustomTabUtils.launchUrl(requireActivity(), "${siteUrl.trimEnd('/')}/$FORGOT_PASSWORD_URL_SUFFIX")
    }
}
