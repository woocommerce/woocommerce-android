package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_ADDONS_BETA_FEATURES_SWITCH_TOGGLED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSettingsBetaBinding
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils

class BetaFeaturesFragment : Fragment(R.layout.fragment_settings_beta) {
    companion object {
        const val TAG = "beta-features"
    }

    private val settingsListener by lazy {
        activity as? AppSettingsListener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentSettingsBetaBinding.bind(view)) {
            bindProductAddonsToggle()
            bindCouponsToggle()
        }
    }

    private fun FragmentSettingsBetaBinding.bindProductAddonsToggle() {
        switchAddonsToggle.isChecked = AppPrefs.isProductAddonsEnabled
        switchAddonsToggle.setOnCheckedChangeListener { switch, isChecked ->
            AnalyticsTracker.track(
                PRODUCT_ADDONS_BETA_FEATURES_SWITCH_TOGGLED,
                mapOf(
                    AnalyticsTracker.KEY_STATE to
                        AnalyticsUtils.getToggleStateLabel(isChecked)
                )
            )

            settingsListener?.onProductAddonsOptionChanged(isChecked)
                ?: handleToggleChangeFailure(switch, isChecked)
        }
    }

    private fun FragmentSettingsBetaBinding.bindCouponsToggle() {
        switchCouponsToggle.isChecked = AppPrefs.isCouponsEnabled
        switchCouponsToggle.setOnCheckedChangeListener { switch, isChecked ->
            settingsListener?.onCouponsOptionChanged(isChecked)
                ?: handleToggleChangeFailure(switch, isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.beta_features)
    }

    private fun FragmentSettingsBetaBinding.handleToggleChangeFailure(switch: CompoundButton, isChecked: Boolean) {
        switch.isChecked = !isChecked
        Snackbar.make(
            mainView,
            R.string.settings_enable_beta_feature_failed_snackbar_text,
            BaseTransientBottomBar.LENGTH_LONG
        ).show()
    }
}
