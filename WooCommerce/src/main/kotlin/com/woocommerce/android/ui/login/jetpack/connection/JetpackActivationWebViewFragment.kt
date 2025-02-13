package com.woocommerce.android.ui.login.jetpack.connection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@AndroidEntryPoint
class JetpackActivationWebViewFragment : BaseFragment() {
    companion object {
        const val JETPACK_CONNECTION_RESULT = "jetpack-connection-result"
    }
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: JetpackActivationWebViewViewModel by viewModels()

    @Inject
    lateinit var authenticator: WebViewAuthenticator

    @Inject
    lateinit var userAgent: UserAgent

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                JetpackActivationWebViewScreen(
                    viewModel = viewModel,
                    authenticator = authenticator,
                    userAgent = userAgent,
                    onUrlLoaded = viewModel::onUrlLoaded,
                    onUrlFailed = viewModel::onUrlFailed,
                    onDismiss = viewModel::onDismiss
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        clearWebViewData()

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is JetpackActivationWebViewViewModel.ConnectionResult -> navigateBackWithResult(
                    key = JETPACK_CONNECTION_RESULT,
                    result = event
                )
            }
        }
    }

    private fun clearWebViewData() {
        WebStorage.getInstance().deleteAllData()

        // Clear all the WebView cookies
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}
