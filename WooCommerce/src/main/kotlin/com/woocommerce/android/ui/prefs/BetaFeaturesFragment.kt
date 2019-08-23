package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import kotlinx.android.synthetic.main.fragment_settings_beta.*

class BetaFeaturesFragment : Fragment() {
    companion object {
        const val TAG = "beta-features"
    }

    private lateinit var settingsListener: AppSettingsListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_beta, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (activity is AppSettingsListener) {
            settingsListener = activity as AppSettingsListener
        } else {
            throw ClassCastException(context.toString() + " must implement AppSettingsListener")
        }

        switchStatsV4UI.isChecked = AppPrefs.isV4StatsUIEnabled()
        switchStatsV4UI.setOnCheckedChangeListener { _, isChecked ->
            // TODO: add analytics events here
            settingsListener.onV4StatsOptionChanged(isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.beta_features)
    }
}
