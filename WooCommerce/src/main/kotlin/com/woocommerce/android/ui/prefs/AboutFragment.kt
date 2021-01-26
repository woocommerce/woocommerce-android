package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.woocommerce.android.AppUrls
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAboutBinding
import com.woocommerce.android.util.ChromeCustomTabUtils
import org.wordpress.android.util.DisplayUtils
import java.util.Calendar

class AboutFragment : Fragment(R.layout.fragment_about) {
    companion object {
        const val TAG = "about"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAboutBinding.bind(view)

        val isLandscape = DisplayUtils.isLandscape(activity)
        binding.aboutImage.visibility = if (isLandscape) {
            View.GONE
        } else {
            View.VISIBLE
        }

        val version = String.format(getString(R.string.about_version), BuildConfig.VERSION_NAME)
        binding.aboutVersion.text = version

        val copyright = String.format(getString(R.string.about_copyright), Calendar.getInstance().get(Calendar.YEAR))
        binding.aboutCopyright.text = copyright

        binding.aboutPrivacy.setOnClickListener {
            ChromeCustomTabUtils.launchUrl(activity as Context, AppUrls.AUTOMATTIC_PRIVACY_POLICY)
        }

        binding.aboutPrivacyCa.setOnClickListener {
            ChromeCustomTabUtils.launchUrl(activity as Context, AppUrls.AUTOMATTIC_PRIVACY_POLICY_CA)
        }

        binding.aboutTos.setOnClickListener {
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
