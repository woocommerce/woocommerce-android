package com.woocommerce.android.ui.settings

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R

class SettingsFragment : Fragment() {
    companion object {
        val TAG: String = SettingsFragment::class.java.simpleName
        fun newInstance() = SettingsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Set the title in the action bar
        activity.title = getString(R.string.wc_settings)
        return inflater?.inflate(R.layout.fragment_settings, container, false)
    }
}
