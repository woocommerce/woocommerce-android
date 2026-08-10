package com.woocommerce.android.support

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MobileStatusReportActivity : ComponentActivity() {
    private val viewModel: MobileStatusReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WooThemeWithBackground {
                val state by viewModel.viewState.collectAsState()
                StatusReportScreen(
                    title = stringResource(id = R.string.support_mobile_status_report),
                    isLoading = state.isLoading,
                    reportText = state.report,
                    copyContentDescription = stringResource(id = R.string.support_mobile_status_report_copy_label),
                    shareContentDescription = stringResource(id = R.string.support_mobile_status_report_share_label),
                    onBackPressed = viewModel::onBackPressed,
                    onCopyButtonClick = viewModel::onCopyButtonClicked,
                    onShareButtonClick = viewModel::onShareButtonClicked
                )
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(this) {
            when (it) {
                is ShareStatusReport -> shareStatusReport(
                    text = it.text,
                    shareErrorMessage = R.string.support_mobile_status_report_share_error
                )

                is CopyStatusReport -> copyStatusReportToClipboard(
                    text = it.text,
                    clipboardLabel = getString(R.string.support_mobile_status_report_clipboard_label),
                    copiedMessage = R.string.support_mobile_status_report_copied_to_clipboard,
                    copyErrorMessage = R.string.support_mobile_status_report_error_copy_to_clipboard
                )

                is Exit -> finish()
                else -> it.isHandled = false
            }
        }
    }
}
