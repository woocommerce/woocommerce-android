package com.woocommerce.android.ui.login.jetpack.main

import android.content.Intent
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
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainViewModel.GoToStore
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainViewModel.ShowJetpackConnectionWebView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JetpackActivationMainFragment : BaseFragment() {
    companion object {
        private const val JETPACK_PLANS_URL = "wordpress.com/jetpack/connect/plans"
    }

    private val viewModel: JetpackActivationMainViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireActivity()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    JetpackActivationMainScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
        setupResultHandlers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowJetpackConnectionWebView -> showConnectionWebView(event.url)
                is GoToStore -> goToStore()
                is Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun goToStore() {
        (requireActivity() as? MainActivity)?.handleSitePickerResult() ?: run {
            val intent = Intent(requireActivity(), MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
    }

    private fun setupResultHandlers() {
        handleNotice(WPComWebViewFragment.WEBVIEW_RESULT) {
            viewModel.onJetpackConnected()
        }
        handleNotice(WPComWebViewFragment.WEBVIEW_DISMISSED) {
            // TODO: setup dismiss state in parent screen
            findNavController().navigateUp()
        }
    }

    private fun showConnectionWebView(connectionUrl: String) {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                urlToLoad = connectionUrl,
                // TODO trigger exit using the site's URL too, depends on:
                //  https://github.com/woocommerce/woocommerce-android/issues/7797
                urlToTriggerExit = JETPACK_PLANS_URL
            )
        )
    }
}
