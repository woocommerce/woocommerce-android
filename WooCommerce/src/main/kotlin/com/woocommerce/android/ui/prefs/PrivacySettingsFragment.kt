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
import com.woocommerce.android.util.ActivityUtils
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_settings_privacy.*
import javax.inject.Inject

class PrivacySettingsFragment : Fragment(), PrivacySettingsContract.View {
    companion object {
        const val TAG = "privacy-settings"
        private const val URL_PRIVACY_POLICY = "https://www.automattic.com/privacy"
        private const val URL_COOKIE_POLICY = "https://www.automattic.com/cookies"

        fun newInstance(): PrivacySettingsFragment {
            return PrivacySettingsFragment()
        }
    }

    @Inject lateinit var presenter: PrivacySettingsContract.Presenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_privacy, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.takeView(this)

        switchSendStats.isChecked = presenter.getSendUsageStats()
        switchSendStats.setOnClickListener {
            presenter.setSendUsageStats(switchSendStats.isChecked)
        }

        buttonLearnMore.setOnClickListener { showCookiePolicy() }
        buttonPrivacyPolicy.setOnClickListener { showPrivacyPolicy() }
        buttonTracking.setOnClickListener { showCookiePolicy() }
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        // Track view shown to user
        AnalyticsTracker.track(Stat.VIEW_SHOWN, mapOf("name" to this::class.java.simpleName))

        activity?.setTitle(R.string.privacy_settings)
    }

    override fun showCookiePolicy() {
        ActivityUtils.openUrlExternal(activity as Context, URL_COOKIE_POLICY)
    }

    override fun showPrivacyPolicy() {
        ActivityUtils.openUrlExternal(activity as Context, URL_PRIVACY_POLICY)
    }
}
