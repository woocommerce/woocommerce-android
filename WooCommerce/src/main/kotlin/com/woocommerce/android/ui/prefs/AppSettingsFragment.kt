package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class AppSettingsFragment : Fragment() {
    companion object {
        const val TAG = "app-settings"

        fun newInstance(): AppSettingsFragment {
            return AppSettingsFragment()
        }
    }

    @Inject internal lateinit var selectedSite: SelectedSite

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
        return inflater.inflate(R.layout.fragment_app_settings, container, false)
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

        textPrimaryStoreDomain.text = UrlUtils.getHost(selectedSite.get().url)
        if (TextUtils.isEmpty(selectedSite.get().username)) {
            textPrimaryStoreUsername.visibility = View.GONE
            val padding = resources.getDimensionPixelSize(R.dimen.settings_padding)
            textPrimaryStoreDomain.setPadding(padding, padding, padding, padding)
        } else {
            textPrimaryStoreUsername.text = selectedSite.get().username
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
