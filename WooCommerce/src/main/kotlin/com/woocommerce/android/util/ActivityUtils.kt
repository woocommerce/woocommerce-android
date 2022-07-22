package com.woocommerce.android.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.util.ToastUtils
import java.io.File

// TODO Duplicating methods from WordPress' WPActivityUtils
object ActivityUtils {
    fun isEmailClientAvailable(context: Context?): Boolean {
        if (context == null) {
            return false
        }

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        val packageManager = context.packageManager
        val emailApps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        return !emailApps.isEmpty()
    }

    fun openEmailClient(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Use this only when you want to open the external browser - otherwise use
     * [ChromeCustomTabUtils.launchUrl] to provide a better in-app experience
     */
    fun openUrlExternal(context: Context, url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, context.getString(R.string.error_cant_open_url), ToastUtils.Duration.LONG)
            WooLog.e(T.UTILS, "No default app available on the device to open the link: $url", e)
        } catch (se: SecurityException) {
            WooLog.e(T.UTILS, "Error opening url in default browser. Url: $url", se)

            val infos = context.packageManager.queryIntentActivities(intent, 0)
            if (infos.size == 1) {
                // there's only one handler and apparently it caused the exception so, just inform and bail
                WooLog.d(T.UTILS, "Only one url handler found so, bailing.")
                ToastUtils.showToast(context, context.getString(R.string.error_cant_open_url))
            } else {
                val chooser = Intent.createChooser(intent, context.getString(R.string.error_please_choose_browser))
                context.startActivity(chooser)
            }
        }
    }

    fun previewPDFFile(activity: Activity, file: File) {
        val pdfUri = FileProvider.getUriForFile(
            activity, "${activity.packageName}.provider", file
        )

        val sendIntent = Intent(Intent.ACTION_VIEW)
        sendIntent.setDataAndType(pdfUri, "application/pdf")
        sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            activity.startActivity(sendIntent)
        } catch (exception: ActivityNotFoundException) {
            ToastUtils.showToast(activity, R.string.shipping_label_preview_pdf_app_missing)
        }
    }

    fun composeEmail(activity: Activity, billingEmail: String, subject: UiString, content: UiString): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(billingEmail))
            putExtra(Intent.EXTRA_SUBJECT, UiHelpers.getTextOfUiString(activity, subject))
            putExtra(Intent.EXTRA_TEXT, UiHelpers.getTextOfUiString(activity, content))
        }
        return try {
            activity.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun shareStoreUrl(context: Context, url: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        val title = context.resources.getText(R.string.share_store_dialog_title)
        context.startActivity(Intent.createChooser(sendIntent, title))
    }
}

fun Context.copyToClipboard(label: String, text: String) {
    with(getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager) {
        setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
