package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.StringUtils
import kotlinx.android.synthetic.main.fragment_licenses.*

class LicensesFragment : androidx.fragment.app.Fragment() {
    companion object {
        const val TAG = "licenses"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_licenses, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        context?.let {
            val prompt = StringUtils.getRawFileUrl(it, R.raw.licenses)
            webView.loadData(prompt, "text/html", "utf-8")
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.let {
            it.title = getString(R.string.settings_licenses)
            (it as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_24dp)
        }
    }
}
