package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSettingsPrivacyBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.PrivacySettingsViewModel.PrivacySettingsEvent.ShowCookiePolicy
import com.woocommerce.android.ui.prefs.PrivacySettingsViewModel.PrivacySettingsEvent.ShowPrivacyPolicy
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrivacySettingsFragment : BaseFragment(R.layout.fragment_settings_privacy) {
    companion object {
        const val TAG = "privacy-settings"
    }

    private val viewModel: PrivacySettingsViewModel by viewModels()

    override fun getFragmentTitle() = getString(R.string.privacy_settings)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsPrivacyBinding.bind(view)

        binding.switchSendStats.isChecked = viewModel.getSendUsageStats()
        binding.switchSendStats.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onSendStatsSettingChanged(isChecked)
        }

        binding.buttonLearnMore.setOnClickListener {
            viewModel.onLearnMoreShareInfoClicked()
        }
        binding.buttonPrivacyPolicy.setOnClickListener {
            viewModel.onPrivacyPolicyClicked()
        }
        binding.buttonTracking.setOnClickListener {
            viewModel.onLearnMoreThirdPartyClicked()
        }

        binding.switchCrashReporting.isChecked = viewModel.getCrashReportingEnabled()
        binding.switchCrashReporting.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onCrashReportingSettingChanged(isChecked)
        }
        observeEvents()
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowCookiePolicy -> showCookiePolicy()
                is ShowPrivacyPolicy -> showPrivacyPolicy()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
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
