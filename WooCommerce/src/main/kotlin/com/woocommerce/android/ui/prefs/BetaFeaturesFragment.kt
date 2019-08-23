package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.fragment_settings_beta.*

class BetaFeaturesFragment : Fragment() {
    companion object {
        const val TAG = "beta-features"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_beta, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        switchStatsV4UI.isChecked = AppPrefs.isV4StatsUIEnabled()
        switchStatsV4UI.setOnClickListener {
            // TODO: add analytics events here
            AppPrefs.setIsV4StatsUIEnabled(switchStatsV4UI.isChecked)
        }
    }
}
