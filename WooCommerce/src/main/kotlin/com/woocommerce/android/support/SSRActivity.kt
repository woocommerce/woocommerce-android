package com.woocommerce.android.support

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class SSRActivity : ComponentActivity() {
    private val viewModel: SSRActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WooThemeWithBackground {
                SSRScreen(viewModel)
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(this) {
            when (it) {
                is ShareSSR -> shareSSR(it.ssrText)
                is CopySSR -> copySSRToClipboard(it.ssrText)
                is ShowSnackbar -> ToastUtils.showToast(this, it.message)
                is Exit -> finish()
                else -> it.isHandled = false
            }
        }
    }

    private fun shareSSR(text: String) {
        shareStatusReport(text, R.string.support_system_status_report_share_error)
    }

    private fun copySSRToClipboard(text: String) {
        val copied = copyStatusReportToClipboard(
            text = text,
            clipboardLabel = getString(R.string.support_system_status_report_clipboard_label),
            copiedMessage = R.string.support_system_status_report_copied_to_clipboard,
            copyErrorMessage = R.string.support_system_status_report_error_copy_to_clipboard
        )
        if (copied) AnalyticsTracker.track(AnalyticsEvent.SUPPORT_SSR_COPY_BUTTON_TAPPED)
    }
}
