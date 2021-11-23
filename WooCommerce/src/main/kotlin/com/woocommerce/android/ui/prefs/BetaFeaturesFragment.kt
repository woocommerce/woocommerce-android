package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_ADDONS_BETA_FEATURES_SWITCH_TOGGLED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_BETA_FEATURES_SIMPLE_PAYMENTS_TOGGLED
import com.woocommerce.android.databinding.FragmentSettingsBetaBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.FeatureFlag

class BetaFeaturesFragment : Fragment(R.layout.fragment_settings_beta) {
    companion object {
        const val TAG = "beta-features"
    }

    private val navArgs: BetaFeaturesFragmentArgs by navArgs()

    private val settingsListener by lazy {
        activity as? AppSettingsListener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentSettingsBetaBinding.bind(view)) {
            bindProductAddonsToggle()
            bindQuickOrdersToggle()
            bindOrderCreationToggle()
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

    private fun FragmentSettingsBetaBinding.bindQuickOrdersToggle() {
        if (FeatureFlag.QUICK_ORDER.isEnabled() && navArgs.isCardReaderOnboardingCompleted) {
            switchQuickOrderToggle.show()
            switchQuickOrderToggle.isChecked = AppPrefs.isQuickOrderEnabled
            switchQuickOrderToggle.setOnCheckedChangeListener { switch, isChecked ->
                AnalyticsTracker.track(
                    SETTINGS_BETA_FEATURES_SIMPLE_PAYMENTS_TOGGLED,
                    mapOf(
                        AnalyticsTracker.KEY_STATE to
                            AnalyticsUtils.getToggleStateLabel(isChecked)
                    )
                )
                settingsListener?.onQuickOrderOptionChanged(isChecked)
                    ?: handleToggleChangeFailure(switch, isChecked)
            }
        } else {
            switchQuickOrderToggle.hide()
        }
    }

    private fun FragmentSettingsBetaBinding.bindOrderCreationToggle() {
        if (FeatureFlag.ORDER_CREATION.isEnabled()) {
            switchOrderCreationToggle.isChecked = AppPrefs.isOrderCreationEnabled
            switchOrderCreationToggle.setOnCheckedChangeListener { switch, isChecked ->
                // trigger order creation tracks

                settingsListener?.onOrderCreationOptionChanged(isChecked)
                    ?: handleToggleChangeFailure(switch, isChecked)
            }
        } else {
            switchOrderCreationToggle.hide()
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
