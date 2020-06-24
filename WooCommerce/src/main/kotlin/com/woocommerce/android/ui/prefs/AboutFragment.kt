package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.AppUrls
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.android.synthetic.main.fragment_about.*
import org.wordpress.android.util.DisplayUtils
import java.util.Calendar

class AboutFragment : androidx.fragment.app.Fragment() {
    companion object {
        const val TAG = "about"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val isLandscape = DisplayUtils.isLandscape(activity)
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
            ChromeCustomTabUtils.launchUrl(activity as Context, AppUrls.AUTOMATTIC_HOME)
        }

        about_privacy.setOnClickListener {
            ChromeCustomTabUtils.launchUrl(activity as Context, AppUrls.AUTOMATTIC_PRIVACY_POLICY)
        }

        about_privacy_ca.setOnClickListener {
            ChromeCustomTabUtils.launchUrl(activity as Context, AppUrls.AUTOMATTIC_PRIVACY_POLICY_CA)
        }

        about_tos.setOnClickListener {
            ChromeCustomTabUtils.launchUrl(activity as Context, AppUrls.AUTOMATTIC_TOS)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.let {
            it.setTitle(R.string.settings_about_title)
            (it as AppCompatActivity).supportActionBar?.elevation = 0f
        }
    }

    override fun onStart() {
        super.onStart()
        ChromeCustomTabUtils.connect(
                activity as Context,
                AppUrls.AUTOMATTIC_PRIVACY_POLICY,
                arrayOf(AppUrls.AUTOMATTIC_TOS, AppUrls.AUTOMATTIC_HOME)
        )
    }

    override fun onStop() {
        super.onStop()
        ChromeCustomTabUtils.disconnect(activity as Context)
    }
}
