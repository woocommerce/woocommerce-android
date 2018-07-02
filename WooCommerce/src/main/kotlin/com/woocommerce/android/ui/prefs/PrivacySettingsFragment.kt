package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R

class PrivacySettingsFragment: Fragment() {
    companion object {
        private const val PRIVACY_POLICY_URL = "https://woocommerce.com/privacy-policy/"
        const val TAG = "privacy_settings"

        fun newInstance(): PrivacySettingsFragment {
            return PrivacySettingsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_privacy_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }
}

