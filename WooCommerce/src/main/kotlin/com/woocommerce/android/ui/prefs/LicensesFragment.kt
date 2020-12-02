package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLicensesBinding
import com.woocommerce.android.util.StringUtils

class LicensesFragment : androidx.fragment.app.Fragment() {
    companion object {
        const val TAG = "licenses"
    }

    private var _binding: FragmentLicensesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLicensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
