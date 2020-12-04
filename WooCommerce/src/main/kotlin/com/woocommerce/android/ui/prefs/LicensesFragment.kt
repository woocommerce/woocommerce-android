package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLicensesBinding
import com.woocommerce.android.util.StringUtils

class LicensesFragment : Fragment(R.layout.fragment_licenses) {
    companion object {
        const val TAG = "licenses"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentLicensesBinding.bind(view)

        context?.let {
            val prompt = StringUtils.getRawFileUrl(it, R.raw.licenses)
            binding.webView.loadData(prompt, "text/html", "utf-8")
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
