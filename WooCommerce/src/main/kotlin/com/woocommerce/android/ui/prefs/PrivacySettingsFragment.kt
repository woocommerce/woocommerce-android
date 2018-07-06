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
        const val TAG = "privacy-settings"
        private const val URL_PRIVACY_POLICY = "https://www.automattic.com/privacy"
        private const val URL_COOKIE_POLICY = "https://www.automattic.com/cookies"

        fun newInstance(): PrivacySettingsFragment {
            return PrivacySettingsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_privacy_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        switchSendStats.isChecked = AnalyticsTracker.sendUsageStats
        switchSendStats.setOnClickListener {
            AnalyticsTracker.sendUsageStats = switchSendStats.isChecked
        }

        buttonLearnMore.setOnClickListener { showCookiePolicy() }
        buttonPrivacyPolicy.setOnClickListener { showPrivacyPolicy() }
        buttonTracking.setOnClickListener { showCookiePolicy() }
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.privacy_settings)
    }

    private fun showCookiePolicy() {
        ActivityUtils.openUrlExternal(activity as Context, URL_COOKIE_POLICY)
    }

    private fun showPrivacyPolicy() {
        ActivityUtils.openUrlExternal(activity as Context, URL_PRIVACY_POLICY)
    }
}
