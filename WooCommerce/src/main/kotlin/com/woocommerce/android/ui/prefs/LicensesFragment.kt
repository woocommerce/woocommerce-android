package com.woocommerce.android.ui.prefs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.webkit.WebView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker

class LicensesFragment : DialogFragment() {
    companion object {
        const val TAG = "licenses"

        fun newInstance(): LicensesFragment {
            return LicensesFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val webView = WebView(context)
        webView.loadUrl("file:///android_asset/licenses.html")

        return AlertDialog.Builder(context)
                .setNegativeButton(R.string.close, null)
                .setView(webView)
                .create()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
