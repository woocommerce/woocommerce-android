package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
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
        observeEvents()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WooThemeWithBackground {
                    PrivacySettingsScreen(viewModel)
                }
            }
        }
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
