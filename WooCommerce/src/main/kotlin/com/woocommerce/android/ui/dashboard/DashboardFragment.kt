package com.woocommerce.android.ui.dashboard

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R

class DashboardFragment : Fragment() {
    companion object {
        val TAG: String = DashboardFragment::class.java.simpleName
        fun newInstance() = DashboardFragment()
    }

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Set the title in the action bar
        activity.title = getString(R.string.wc_dashboard)
        return inflater?.inflate(R.layout.fragment_dashboard, container, false)
    }
}
