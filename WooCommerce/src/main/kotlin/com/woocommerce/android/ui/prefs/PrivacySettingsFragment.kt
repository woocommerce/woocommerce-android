package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.PrivacySettingsViewModel.PrivacySettingsEvent.OpenPolicies
import com.woocommerce.android.ui.prefs.PrivacySettingsViewModel.PrivacySettingsEvent.ShowUsageTracker
import com.woocommerce.android.ui.prefs.PrivacySettingsViewModel.PrivacySettingsEvent.ShowWebOptions
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrivacySettingsFragment : BaseFragment() {
    companion object {
        const val TAG = "privacy-settings"
    }

    private val viewModel: PrivacySettingsViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private var snackbar: Snackbar? = null

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
                is ShowWebOptions -> showWebOptions()
                is ShowUsageTracker -> showUsageTracker()
                is OpenPolicies -> findNavController().navigateSafely(
                    PrivacySettingsFragmentDirections.actionPrivacySettingsFragmentToPrivacySettingsPolicesFragment()
                )
                is MultiLiveEvent.Event.ShowActionSnackbar ->
                    snackbar = uiMessageResolver.getIndefiniteActionSnack(
                        event.message,
                        actionText = event.actionText,
                        actionListener = event.action
                    ).apply {
                        show()
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onPause() {
        super.onPause()
        snackbar?.dismiss()
    }

    private fun showWebOptions() {
        ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.WOOCOMMERCE_WEB_OPTIONS)
    }

    private fun showUsageTracker() {
        ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.WOOCOMMERCE_USAGE_TRACKER)
    }
}
