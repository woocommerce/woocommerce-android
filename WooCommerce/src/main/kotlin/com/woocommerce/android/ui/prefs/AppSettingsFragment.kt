package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.fragment_app_settings.*

class AppSettingsFragment : Fragment() {
    companion object {
        const val TAG = "app-settings"

        fun newInstance(): AppSettingsFragment {
            return AppSettingsFragment()
        }
    }

    interface AppSettingsListener {
        fun onRequestLogout()
        fun onRequestShowPrivacySettings()
    }

    private lateinit var listener: AppSettingsListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is AppSettingsListener) {
            listener = activity as AppSettingsListener
        } else {
            throw ClassCastException(context.toString() + " must implement AppSettingsListener")
        }

        buttonLogout.setOnClickListener {
            listener.onRequestLogout()
        }

        textPrivacySettings.setOnClickListener {
            listener.onRequestShowPrivacySettings()
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.settings)
    }
}
