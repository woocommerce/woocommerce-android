package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRIVACY_SETTINGS_COLLECT_INFO_TOGGLED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRIVACY_SETTINGS_CRASH_REPORTING_TOGGLED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRIVACY_SETTINGS_SHARE_INFO_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRIVACY_SETTINGS_THIRD_PARTY_TRACKING_INFO_LINK_TAPPED
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.AnalyticsUtils
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_settings_privacy.*
import javax.inject.Inject

class PrivacySettingsFragment : Fragment(), PrivacySettingsContract.View {
    companion object {
        const val TAG = "privacy-settings"
        private const val URL_PRIVACY_POLICY = "https://www.automattic.com/privacy"
        private const val URL_COOKIE_POLICY = "https://www.automattic.com/cookies"

        fun newInstance(): PrivacySettingsFragment {
            return PrivacySettingsFragment()
        }
    }

    @Inject lateinit var presenter: PrivacySettingsContract.Presenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_privacy, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.takeView(this)

        switchSendStats.isChecked = presenter.getSendUsageStats()
        switchSendStats.setOnClickListener {
            AnalyticsTracker.track(PRIVACY_SETTINGS_COLLECT_INFO_TOGGLED, mapOf(
                    AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(switchSendStats.isChecked)))
            presenter.setSendUsageStats(switchSendStats.isChecked)
        }

        buttonLearnMore.setOnClickListener {
            AnalyticsTracker.track(PRIVACY_SETTINGS_SHARE_INFO_LINK_TAPPED)
            showCookiePolicy()
        }
        buttonPrivacyPolicy.setOnClickListener {
            AnalyticsTracker.track(PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED)
            showPrivacyPolicy()
        }
        buttonTracking.setOnClickListener {
            AnalyticsTracker.track(PRIVACY_SETTINGS_THIRD_PARTY_TRACKING_INFO_LINK_TAPPED)
            showCookiePolicy()
        }

        switchCrashReporting.isChecked = presenter.getCrashReportingEnabled()
        switchCrashReporting.setOnClickListener {
            AnalyticsTracker.track(
                    PRIVACY_SETTINGS_CRASH_REPORTING_TOGGLED, mapOf(
                    AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(switchCrashReporting.isChecked)))
            presenter.setCrashReportingEnabled(activity!!, switchCrashReporting.isChecked)
        }
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.privacy_settings)
    }

    override fun showCookiePolicy() {
        ActivityUtils.openUrlExternal(activity as Context, URL_COOKIE_POLICY)
    }

    override fun showPrivacyPolicy() {
        ActivityUtils.openUrlExternal(activity as Context, URL_PRIVACY_POLICY)
    }
}
