package com.woocommerce.android.ui.login.jetpack.sitecredentials

import android.content.Intent
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
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.jetpack.sitecredentials.JetpackActivationSiteCredentialsViewModel.NavigateToJetpackActivationSteps
import com.woocommerce.android.ui.login.jetpack.sitecredentials.JetpackActivationSiteCredentialsViewModel.OpenWordPressComLogin
import com.woocommerce.android.ui.login.jetpack.sitecredentials.JetpackActivationSiteCredentialsViewModel.ResetPassword
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginMode
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
                is OpenWordPressComLogin -> openWordPressComLogin(event.email)
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
                    jetpackStatus = event.jetpackStatus,
                    siteUrl = event.siteUrl
                )
        )
    }

    private fun openWordPressComLogin(email: String) {
        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            action = LoginActivity.LOGIN_WITH_WPCOM_EMAIL_ACTION
            putExtra(LoginActivity.EMAIL_PARAMETER, email)
            LoginMode.WOO_LOGIN_MODE.putInto(this)
        }
        startActivity(intent)
    }

    private fun showResetPasswordWebPage(siteUrl: String) {
        ChromeCustomTabUtils.launchUrl(requireActivity(), "${siteUrl.trimEnd('/')}/$FORGOT_PASSWORD_URL_SUFFIX")
    }
}
