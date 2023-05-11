package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.PrivacySettingsViewModel.PrivacySettingsEvent.ShowAdvertisingOptions
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrivacySettingsFragment : BaseFragment() {
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
                is ShowAdvertisingOptions -> showAdvertisingOptions()
                is PrivacySettingsViewModel.PrivacySettingsEvent.OpenPolicies -> findNavController().navigateSafely(
                    PrivacySettingsFragmentDirections.actionPrivacySettingsFragmentToPrivacySettingsPolicesFragment()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun showAdvertisingOptions() {
        ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.AUTOMATTIC_ADVERTISING_OPTIONS)
    }
}
