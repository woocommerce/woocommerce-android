package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.ActivityUtils
import kotlinx.android.synthetic.main.fragment_about.*
import org.wordpress.android.util.DisplayUtils
import java.util.Calendar

class AboutFragment : Fragment() {
    companion object {
        const val TAG = "about"
        private const val URL_AUTOMATTIC = "https://www.automattic.com/"
        private const val URL_PRIVACY_POLICY = "https://www.automattic.com/privacy"
        private const val URL_TOS = "https://woocommerce.com/terms-conditions/"

        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val isLandscape = DisplayUtils.isLandscape(activity)
        about_container.gravity = if (isLandscape) {
            Gravity.CENTER_HORIZONTAL
        } else {
            Gravity.CENTER
        }
        about_image.visibility = if (isLandscape) {
            View.GONE
        } else {
            View.VISIBLE
        }

        val version = String.format(getString(R.string.about_version), BuildConfig.VERSION_NAME)
        about_version.text = version

        val copyright = String.format(getString(R.string.about_copyright), Calendar.getInstance().get(Calendar.YEAR))
        about_copyright.text = copyright

        about_url.setOnClickListener {
            ActivityUtils.openUrlExternal(activity as Context, URL_AUTOMATTIC)
        }

        about_privacy.setOnClickListener {
            ActivityUtils.openUrlExternal(activity as Context, URL_PRIVACY_POLICY)
        }

        about_tos.setOnClickListener {
            ActivityUtils.openUrlExternal(activity as Context, URL_TOS)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.let {
            it.title = null
            (it as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
            // it.supportActionBar?.elevation = 0f
        }
    }
}
