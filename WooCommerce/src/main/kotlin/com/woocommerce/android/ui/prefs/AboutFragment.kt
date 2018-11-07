package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED
import com.woocommerce.android.util.ActivityUtils
import kotlinx.android.synthetic.main.fragment_about.*

class AboutFragment : Fragment() {
    companion object {
        const val TAG = "about"
        private const val URL_PRIVACY_POLICY = "https://www.automattic.com/privacy"

        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        about_privacy.setOnClickListener {
            AnalyticsTracker.track(PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED)
            showPrivacyPolicy()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        activity?.setTitle(null)
        with (activity as AppCompatActivity) {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
        }
    }

    fun showPrivacyPolicy() {
        ActivityUtils.openUrlExternal(activity as Context, URL_PRIVACY_POLICY)
    }
}
