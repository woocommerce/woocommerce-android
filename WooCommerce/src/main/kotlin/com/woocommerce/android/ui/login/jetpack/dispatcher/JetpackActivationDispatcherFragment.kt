package com.woocommerce.android.ui.login.jetpack.dispatcher

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.viewModels
import androidx.navigation.NavGraph
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.login.jetpack.dispatcher.JetpackActivationDispatcherViewModel.StartJetpackActivationForNewSite
import com.woocommerce.android.ui.login.jetpack.dispatcher.JetpackActivationDispatcherViewModel.StartWPComAuthenticationForEmail
import com.woocommerce.android.ui.login.jetpack.dispatcher.JetpackActivationDispatcherViewModel.StartWPComLoginForJetpackActivation
import com.woocommerce.android.ui.login.wpcom.WPComLoginEmailFragmentArgs
import com.woocommerce.android.ui.login.wpcom.WPComLoginPasswordFragmentArgs
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
                is StartWPComLoginForJetpackActivation -> navigateToWPComLoginGraph(
                    startDestination = R.id.wPComLoginEmailFragment,
                    args = WPComLoginEmailFragmentArgs(
                        jetpackStatus = event.jetpackStatus
                    ).toBundle()
                )
                is StartWPComAuthenticationForEmail -> navigateToWPComLoginGraph(
                    startDestination = R.id.wPComLoginPasswordFragment,
                    args = WPComLoginPasswordFragmentArgs(
                        jetpackStatus = event.jetpackStatus,
                        emailOrUsername = event.wpComEmail
                    ).toBundle()
                )
            }
        }
    }

    private fun navigateToJetpackActivationStartScreen(event: StartJetpackActivationForNewSite) {
        findNavController().navigate(
            JetpackActivationDispatcherFragmentDirections
                .actionJetpackActivationDispatcherFragmentToJetpackActivationStartFragment(
                    siteUrl = event.siteUrl,
                    jetpackStatus = event.jetpackStatus
                )
        )
    }

    private fun navigateToWPComLoginGraph(@IdRes startDestination: Int, args: Bundle) {
        val navController = findNavController()
        val wpComLoginGraph = navController.currentDestination?.parent
            ?.findNode(R.id.nav_graph_wpcom_login) as? NavGraph ?: return
        wpComLoginGraph.setStartDestination(startDestination)

        navController.navigateSafely(
            R.id.action_jetpackActivationDispatcherFragment_to_nav_graph_wpcom_login,
            args
        )
    }
}
