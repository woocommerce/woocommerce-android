package com.woocommerce.android.support

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import com.woocommerce.android.R
import com.woocommerce.android.extensions.copyToClipboard
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils
import java.io.File

@AndroidEntryPoint
class WooLogViewerActivity : ComponentActivity() {
    private val viewModel: WooLogViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WooTheme {
                WooLogViewerScreen(viewModel)
            }
        }

        viewModel.event.observe(this) { event ->
            when (event) {
                is WooLogViewerViewModel.ShareLogs -> shareAppLog(event.logs)
                is WooLogViewerViewModel.CopyLogs -> copyAppLogToClipboard(event.logs)
                is WooLogViewerViewModel.ShareLogsArchive -> shareAppLogArchive(event.archive)
                is WooLogViewerViewModel.ShareLogsArchiveFailed ->
                    ToastUtils.showToast(this, R.string.logviewer_share_all_logs_error)
            }
        }
    }

    private fun shareAppLogArchive(archive: File) {
        val archiveUri = FileProvider.getUriForFile(this, "$packageName.provider", archive)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/zip"
        intent.putExtra(Intent.EXTRA_STREAM, archiveUri)
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + title)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (_: android.content.ActivityNotFoundException) {
            ToastUtils.showToast(this, R.string.logviewer_share_error)
        }
    }

    private fun shareAppLog(logs: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, logs)
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + title)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (_: android.content.ActivityNotFoundException) {
            ToastUtils.showToast(this, R.string.logviewer_share_error)
        }
    }

    private fun copyAppLogToClipboard(logs: String) {
        try {
            copyToClipboard("AppLog", logs)
            ToastUtils.showToast(this, R.string.logviewer_copied_to_clipboard)
        } catch (e: Exception) {
            WooLog.e(T.UTILS, e)
            ToastUtils.showToast(this, R.string.logviewer_error_copy_to_clipboard)
        }
    }
}
