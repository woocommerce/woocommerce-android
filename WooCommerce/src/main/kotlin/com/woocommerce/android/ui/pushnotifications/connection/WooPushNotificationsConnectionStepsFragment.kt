package com.woocommerce.android.ui.pushnotifications.connection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewFragment
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewFragmentArgs
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewViewModel
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.ShowJetpackConnectionWebView
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
                is NavigateToHelpScreen -> navigateToHelpScreen(event.origin)
                is Exit -> findNavController().navigateUp()
                is ShowJetpackConnectionWebView -> showConnectionWebView(event)
            }
        }
    }

    private fun setupResultHandlers() {
        handleResult<JetpackActivationWebViewViewModel.ConnectionResult>(
            key = JetpackActivationWebViewFragment.JETPACK_CONNECTION_RESULT
        ) {
            viewModel.onJetpackConnectionResult(it)
        }
    }

    private fun showConnectionWebView(event: ShowJetpackConnectionWebView) {
        findNavController().navigateSafely(
            resId = R.id.jetpackActivationWebViewFragment,
            bundle = JetpackActivationWebViewFragmentArgs(urlToLoad = event.url).toBundle(),
            navOptions = NavOptions.Builder()
                .setEnterAnim(R.anim.slide_up)
                .setExitAnim(R.anim.no_anime)
                .setPopEnterAnim(R.anim.no_anime)
                .setPopExitAnim(R.anim.slide_down)
                .build()
        )
    }
}
