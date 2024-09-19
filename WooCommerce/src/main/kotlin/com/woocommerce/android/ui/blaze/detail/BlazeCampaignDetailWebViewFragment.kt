package com.woocommerce.android.ui.blaze.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewScreen
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.userAgent
import javax.inject.Inject

@AndroidEntryPoint
class BlazeCampaignDetailWebViewFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: BlazeCampaignDetailWebViewViewModel by viewModels()

    @Inject
    lateinit var wpComAuthenticator: WPComWebViewAuthenticator

    @Inject
    lateinit var userAgent: UserAgent

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    BlazeCampaignDetailWebViewScreen(
                        viewModel = viewModel,
                        wpComAuthenticator = wpComAuthenticator,
                        userAgent = userAgent,
                        onUrlLoaded = viewModel::onUrlLoaded,
                        onDismiss = viewModel::onDismiss
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> navigateBackWithNotice(WEBVIEW_RESULT)
                is Exit -> navigateBackWithNotice(WEBVIEW_DISMISSED)
            }
        }
    }
}
