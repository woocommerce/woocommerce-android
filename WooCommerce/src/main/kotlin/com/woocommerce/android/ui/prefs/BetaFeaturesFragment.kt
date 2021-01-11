package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_BETA_FEATURES_PRODUCTS_TOGGLED
import com.woocommerce.android.databinding.FragmentSettingsBetaBinding
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils

class BetaFeaturesFragment : Fragment(R.layout.fragment_settings_beta) {
    companion object {
        const val TAG = "beta-features"
    }

    private lateinit var settingsListener: AppSettingsListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSettingsBetaBinding.bind(view)

        if (activity is AppSettingsListener) {
            settingsListener = activity as AppSettingsListener
        } else {
            throw ClassCastException(context.toString() + " must implement AppSettingsListener")
        }

        binding.switchProductsUI.isChecked = AppPrefs.isProductsFeatureEnabled()
        binding.switchProductsUI.setOnCheckedChangeListener { _, isChecked ->
            AnalyticsTracker.track(
                    SETTINGS_BETA_FEATURES_PRODUCTS_TOGGLED, mapOf(
                    AnalyticsTracker.KEY_STATE to
                        AnalyticsUtils.getToggleStateLabel(binding.switchProductsUI.isChecked)))
            settingsListener.onProductsFeatureOptionChanged(isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.beta_features)
    }
}
