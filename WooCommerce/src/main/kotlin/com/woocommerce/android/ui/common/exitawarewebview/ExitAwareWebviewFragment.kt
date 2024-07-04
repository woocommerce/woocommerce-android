package com.woocommerce.android.ui.common.exitawarewebview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExitAwareWebviewFragment: BaseFragment(R.layout.fragment_exitaware_webview) {
    companion object {
        const val WEBVIEW_RESULT = "webview-result"
        const val WEBVIEW_DISMISSED = "webview-dismissed"
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    // add view model

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    // add component
                }
            }
        }
    }

    // add view model event observer
}
