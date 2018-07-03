package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.ActivityUtils
import kotlinx.android.synthetic.main.fragment_privacy_settings.*

class PrivacySettingsFragment : Fragment() {
    companion object {
        private const val PRIVACY_POLICY_URL = "https://woocommerce.com/privacy-policy/"
        const val TAG = "privacy_settings"

        fun newInstance(): PrivacySettingsFragment {
            return PrivacySettingsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_privacy_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        switchCollectInformation.isChecked = AnalyticsTracker.sendUsageStats
        switchCollectInformation.setOnClickListener {
            AnalyticsTracker.sendUsageStats = switchCollectInformation.isChecked
        }

        buttonPrivacyPolicy.setOnClickListener {
            showPrivacyPolicy()
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.privacy_settings)
    }

    private fun showPrivacyPolicy() {
        ActivityUtils.openUrlExternal(activity as Context, PRIVACY_POLICY_URL)
    }
}
