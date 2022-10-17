package com.woocommerce.android.support

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.copyToClipboard
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils
import java.lang.IllegalStateException

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
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + title)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: android.content.ActivityNotFoundException) {
            WooLog.e(T.UTILS, e)
            ToastUtils.showToast(this, R.string.support_system_status_report_share_error)
        }
    }

    private fun copySSRToClipboard(text: String) {
        try {
            copyToClipboard(getString(R.string.support_system_status_report_clipboard_label), text)
            AnalyticsTracker.track(AnalyticsEvent.SUPPORT_SSR_COPY_BUTTON_TAPPED)
            ToastUtils.showToast(this, R.string.support_system_status_report_copied_to_clipboard)
        } catch (e: IllegalStateException) {
            WooLog.e(T.UTILS, e)
            ToastUtils.showToast(this, R.string.support_system_status_report_error_copy_to_clipboard)
        }
    }
}
