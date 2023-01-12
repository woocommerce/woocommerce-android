package com.woocommerce.android.ui.login.jetpack.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainFragment
import com.woocommerce.android.ui.login.jetpack.start.JetpackActivationStartViewModel.ContinueJetpackConnection
import com.woocommerce.android.ui.login.jetpack.start.JetpackActivationStartViewModel.NavigateToSiteCredentialsScreen
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JetpackActivationStartFragment : BaseFragment() {
    private val viewModel: JetpackActivationStartViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    JetpackActivationStartScreen(viewModel = viewModel)
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
                is NavigateToSiteCredentialsScreen -> navigateToSiteCredentialsScreen(event)
                is ContinueJetpackConnection -> continueJetpackConnection(event)
                is NavigateToHelpScreen -> navigateToHelpScreen(event.origin)
                Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun handleResults() {
        handleNotice(JetpackActivationMainFragment.CONNECTION_DISMISSED_RESULT) {
            viewModel.onConnectionDismissed()
        }
    }

    private fun navigateToSiteCredentialsScreen(event: NavigateToSiteCredentialsScreen) {
        findNavController().navigateSafely(
            JetpackActivationStartFragmentDirections
                .actionJetpackActivationStartFragmentToJetpackActivationSiteCredentialsFragment(
                    siteUrl = event.siteUrl,
                    isJetpackInstalled = event.isJetpackInstalled
                )
        )
    }

    private fun continueJetpackConnection(event: ContinueJetpackConnection) {
        findNavController().navigateSafely(
            JetpackActivationStartFragmentDirections
                .actionJetpackActivationStartFragmentToJetpackActivationMainFragment(
                    siteUrl = event.siteUrl,
                    isJetpackInstalled = true
                )
        )
    }
}
