package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.PRIVACY_SETTINGS_COLLECT_INFO_TOGGLED
import com.woocommerce.android.analytics.AnalyticsEvent.PRIVACY_SETTINGS_CRASH_REPORTING_TOGGLED
import com.woocommerce.android.analytics.AnalyticsEvent.PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRIVACY_SETTINGS_SHARE_INFO_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRIVACY_SETTINGS_THIRD_PARTY_TRACKING_INFO_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSettingsPrivacyBinding
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrivacySettingsFragment : Fragment(R.layout.fragment_settings_privacy) {
    companion object {
        const val TAG = "privacy-settings"
    }

    private val viewModel: PrivacySettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsPrivacyBinding.bind(view)

        binding.switchSendStats.isChecked = viewModel.getSendUsageStats()
        binding.switchSendStats.setOnCheckedChangeListener { _, isChecked ->
            AnalyticsTracker.track(
                PRIVACY_SETTINGS_COLLECT_INFO_TOGGLED,
                mapOf(
                    AnalyticsTracker.KEY_STATE to
                        AnalyticsUtils.getToggleStateLabel(binding.switchSendStats.isChecked)
                )
            )
            viewModel.setSendUsageStats(isChecked)
        }

        binding.buttonLearnMore.setOnClickListener {
            AnalyticsTracker.track(PRIVACY_SETTINGS_SHARE_INFO_LINK_TAPPED)
            showCookiePolicy()
        }
        binding.buttonPrivacyPolicy.setOnClickListener {
            AnalyticsTracker.track(PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED)
            showPrivacyPolicy()
        }
        binding.buttonTracking.setOnClickListener {
            AnalyticsTracker.track(PRIVACY_SETTINGS_THIRD_PARTY_TRACKING_INFO_LINK_TAPPED)
            showCookiePolicy()
        }

        binding.switchCrashReporting.isChecked = viewModel.getCrashReportingEnabled()
        binding.switchCrashReporting.setOnCheckedChangeListener { _, isChecked ->
            AnalyticsTracker.track(
                PRIVACY_SETTINGS_CRASH_REPORTING_TOGGLED,
                mapOf(
                    AnalyticsTracker.KEY_STATE to
                        AnalyticsUtils.getToggleStateLabel(binding.switchCrashReporting.isChecked)
                )
            )
            viewModel.setCrashReportingEnabled(isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.privacy_settings)
    }

    override fun onStart() {
        super.onStart()
        ChromeCustomTabUtils.connectAndStartSession(
            activity as Context,
            AppUrls.AUTOMATTIC_PRIVACY_POLICY,
            arrayOf(AppUrls.AUTOMATTIC_COOKIE_POLICY)
        )
    }

    override fun onStop() {
        super.onStop()
        ChromeCustomTabUtils.disconnect(activity as Context)
    }

    private fun showCookiePolicy() {
        ChromeCustomTabUtils.launchUrl(activity as Context, AppUrls.AUTOMATTIC_COOKIE_POLICY)
    }

    private fun showPrivacyPolicy() {
        ChromeCustomTabUtils.launchUrl(activity as Context, AppUrls.AUTOMATTIC_PRIVACY_POLICY)
    }
}
