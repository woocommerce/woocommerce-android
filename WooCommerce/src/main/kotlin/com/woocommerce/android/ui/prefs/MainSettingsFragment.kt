package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_settings_main.*
import javax.inject.Inject

class MainSettingsFragment : Fragment(), MainSettingsContract.View {
    companion object {
        const val TAG = "main-settings"

        fun newInstance(): MainSettingsFragment {
            return MainSettingsFragment()
        }
    }

    @Inject lateinit var presenter: MainSettingsContract.Presenter

    interface AppSettingsListener {
        fun onRequestLogout()
        fun onRequestShowPrivacySettings()
    }

    private lateinit var listener: AppSettingsListener

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is AppSettingsListener) {
            listener = activity as AppSettingsListener
        } else {
            throw ClassCastException(context.toString() + " must implement AppSettingsListener")
        }

        textPrimaryStoreDomain.text = presenter.getStoreDomainName()
        textPrimaryStoreUsername.text = presenter.getUserDisplayName()

        buttonLogout.setOnClickListener {
            listener.onRequestLogout()
        }

        textPrivacySettings.setOnClickListener {
            listener.onRequestShowPrivacySettings()
        }
    }

    override fun onResume() {
        super.onResume()

        // Track view shown to user
        AnalyticsTracker.track(Stat.VIEW_SHOWN, mapOf("name" to this::class.java.simpleName))

        activity?.setTitle(R.string.settings)
    }
}
