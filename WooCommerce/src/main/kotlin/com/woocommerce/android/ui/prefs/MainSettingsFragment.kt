package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.woocommerce.android.R
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class MainSettingsFragment : Fragment(), MainSettingsFragmentContract.View {
    companion object {
        const val TAG = "app-settings"

        fun newInstance(): MainSettingsFragment {
            return MainSettingsFragment()
        }
    }

    @Inject lateinit var presenter: MainSettingsFragmentContract.Presenter

    interface AppSettingsListener {
        fun onRequestLogout()
        fun onRequestShowPrivacySettings()
    }

    private lateinit var listener: AppSettingsListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
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

        // TODO: replace with synthetics once Kotlin plugin bug is fixed
        val textPrimaryStoreDomain = view!!.findViewById<TextView>(R.id.textPrimaryStoreDomain)
        val textPrimaryStoreUsername = view!!.findViewById<TextView>(R.id.textPrimaryStoreUsername)
        val textPrivacySettings = view!!.findViewById<TextView>(R.id.textPrivacySettings)
        val buttonLogout = view!!.findViewById<Button>(R.id.buttonLogout)

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
        activity?.setTitle(R.string.settings)
    }
}
