package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.POS_LOCAL_CATALOG_BETA_FEATURES_SWITCH_TOGGLED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_ADDONS_BETA_FEATURES_SWITCH_TOGGLED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSettingsBetaBinding
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BetaFeaturesFragment : Fragment(R.layout.fragment_settings_beta) {
    companion object {
        const val TAG = "beta-features"
    }

    @Inject
    lateinit var selectedSite: SelectedSite

    @Inject
    lateinit var featureFlagRepository: FeatureFlagRepository

    private val settingsListener by lazy {
        activity as? AppSettingsListener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentSettingsBetaBinding.bind(view)) {
            bindProductAddonsToggle()
            bindJetpackAppPasswordsToggle()
            bindWooPosLocalCatalogToggle()
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

    private fun FragmentSettingsBetaBinding.bindJetpackAppPasswordsToggle() {
        viewLifecycleOwner.lifecycleScope.launch {
            val isJetpackSite = selectedSite.connectionType != SiteConnectionType.ApplicationPasswords
            jetpackAppPasswordsToggle.isVisible = isJetpackSite &&
                featureFlagRepository.isEnabled(FeatureFlag.APP_PASSWORDS_FOR_JETPACK_SITES)
        }

        jetpackAppPasswordsToggle.isChecked = AppPrefs.jetpackAppPasswordsEnabled
        jetpackAppPasswordsToggle.setOnCheckedChangeListener { _, isChecked ->
            AppPrefs.jetpackAppPasswordsEnabled = isChecked
        }
    }

    private fun FragmentSettingsBetaBinding.bindWooPosLocalCatalogToggle() {
        viewLifecycleOwner.lifecycleScope.launch {
            wooPosLocalCatalogToggle.isVisible =
                featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_LOCAL_CATALOG_M1)
        }

        wooPosLocalCatalogToggle.isChecked = AppPrefs.wooPosLocalCatalogEnabled
        wooPosLocalCatalogToggle.setOnCheckedChangeListener { _, isChecked ->
            AnalyticsTracker.track(
                POS_LOCAL_CATALOG_BETA_FEATURES_SWITCH_TOGGLED,
                mapOf(AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(isChecked))
            )
            AppPrefs.wooPosLocalCatalogEnabled = isChecked
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.experimental_features)
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
