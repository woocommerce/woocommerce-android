package com.woocommerce.android.ui.login.storecreation.installation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.NavigateToNewStore
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.OpenStore
import com.woocommerce.android.ui.login.storecreation.theme.ThemeActivationFragmentDialog
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@AndroidEntryPoint
class StoreInstallationFragment : BaseFragment() {
    private val viewModel: StoreInstallationViewModel by viewModels()

    @Inject
    lateinit var userAgent: UserAgent

    @Inject
    lateinit var authenticator: WPComWebViewAuthenticator

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(viewModel.performanceObserver)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    StoreInstallationScreen(viewModel, userAgent, authenticator)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        handleResults()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().popBackStack()
                is OpenStore -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                is StoreInstallationViewModel.LaunchThemeActivation -> launchThemeActivation(event.themeId)
                NavigateToNewStore -> (activity as? MainActivity)?.handleSitePickerResult()
            }
        }
    }

    private fun handleResults() {
        handleNotice(ThemeActivationFragmentDialog.THEME_ACTIVATION_FINISHED) {
            viewModel.onThemeActivationFinished()
        }
    }

    private fun launchThemeActivation(themeId: String) {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalThemeActivationFragmentDialog(themeId)
        )
    }
}
