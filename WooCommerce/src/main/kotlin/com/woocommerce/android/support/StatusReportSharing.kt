package com.woocommerce.android.support

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.extensions.copyToClipboard
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.util.ToastUtils

/**
 * Copy and share plumbing for the status report screens. Both reports are plain text handled identically, so the
 * only thing that varies is which strings the merchant is shown.
 */
fun ComponentActivity.shareStatusReport(text: String, @StringRes shareErrorMessage: Int) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, text)
    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + title)
    try {
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    } catch (e: ActivityNotFoundException) {
        WooLog.e(T.UTILS, e)
        ToastUtils.showToast(this, shareErrorMessage)
    }
}

/**
 * @return whether the text reached the clipboard, so callers can track a copy only when one happened.
 */
fun ComponentActivity.copyStatusReportToClipboard(
    text: String,
    clipboardLabel: String,
    @StringRes copiedMessage: Int,
    @StringRes copyErrorMessage: Int
): Boolean =
    try {
        copyToClipboard(clipboardLabel, text)
        ToastUtils.showToast(this, copiedMessage)
        true
    } catch (e: IllegalStateException) {
        WooLog.e(T.UTILS, e)
        ToastUtils.showToast(this, copyErrorMessage)
        false
    }
