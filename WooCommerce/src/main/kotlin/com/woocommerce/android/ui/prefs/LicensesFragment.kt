package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker

class LicensesFragment : Fragment() {
    companion object {
        const val TAG = "licenses"

        fun newInstance(): LicensesFragment {
            return LicensesFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val webView = WebView(context)
        webView.loadUrl("file:///android_asset/licenses.html")
        return webView
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.let {
            it.title = getString(R.string.settings_licenses)
            (it as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
        }
    }
}
