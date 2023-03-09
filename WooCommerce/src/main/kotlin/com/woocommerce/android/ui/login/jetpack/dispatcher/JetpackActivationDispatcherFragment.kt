package com.woocommerce.android.ui.login.jetpack.dispatcher

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.login.jetpack.dispatcher.JetpackActivationDispatcherViewModel.StartJetpackActivationForNewSite
import com.woocommerce.android.ui.login.jetpack.dispatcher.JetpackActivationDispatcherViewModel.StartWPComAuthenticationForEmail
import com.woocommerce.android.ui.login.jetpack.dispatcher.JetpackActivationDispatcherViewModel.StartWPComLoginForJetpackActivation
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint

/**
 * An empty screen that allows dispatching between the different flows of
 * the Jetpack Activation (Installation and connection)
 */
@AndroidEntryPoint
class JetpackActivationDispatcherFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: JetpackActivationDispatcherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the fragment as the lifecycle owner since we don't have any view here
        viewModel.event.observe(this) { event ->
            when (event) {
                is StartJetpackActivationForNewSite -> navigateToJetpackActivationStartScreen(event)
                is StartWPComLoginForJetpackActivation -> navigateToWPComEmailScreen(event)
                is StartWPComAuthenticationForEmail -> navigateToWPComPasswordScreen(event)
            }
        }
    }

    private fun navigateToJetpackActivationStartScreen(event: StartJetpackActivationForNewSite) {
        findNavController().navigate(
            JetpackActivationDispatcherFragmentDirections
                .actionJetpackActivationDispatcherFragmentToJetpackActivationStartFragment(
                    siteUrl = event.siteUrl,
                    isJetpackInstalled = event.isJetpackInstalled
                )
        )
    }

    private fun navigateToWPComEmailScreen(event: StartWPComLoginForJetpackActivation) {
        findNavController().navigate(
            JetpackActivationDispatcherFragmentDirections
                .actionJetpackActivationDispatcherFragmentToJetpackActivationWPComEmailFragment(
                    jetpackStatus = event.jetpackStatus
                )
        )
    }

    private fun navigateToWPComPasswordScreen(event: StartWPComAuthenticationForEmail) {
        findNavController().navigate(
            JetpackActivationDispatcherFragmentDirections
                .actionJetpackActivationDispatcherFragmentToJetpackActivationWPComPasswordFragment(
                    emailOrUsername = event.wpComEmail,
                    jetpackStatus = event.jetpackStatus
                )
        )
    }
}
