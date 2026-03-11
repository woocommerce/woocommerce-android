package com.woocommerce.android.ui.pushnotifications.connection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.webview.AuthenticatedWebViewFragment
import com.woocommerce.android.ui.common.webview.AuthenticatedWebViewViewModel
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooPushNotificationsConnectionStepsFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: WooPushNotificationsConnectionStepsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            WooPushNotificationsConnectionStepsScreen(viewModel = viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
        setupResultHandlers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is WooPushNotificationsConnectionStepsViewModel.NavigateToPluginUpdatePage -> {
                    findNavController().navigateSafely(
                        NavGraphMainDirections.actionGlobalAuthenticatedWebViewFragment(
                            urlToLoad = event.url,
                            displayMode = AuthenticatedWebViewViewModel.DisplayMode.TASK
                        )
                    )
                }
                is NavigateToHelpScreen -> navigateToHelpScreen(event.origin)
                is Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun setupResultHandlers() {
        handleNotice(AuthenticatedWebViewFragment.WEBVIEW_DISMISSED) {
            viewModel.onPluginUpdateWebViewDismissed()
        }
    }
}
