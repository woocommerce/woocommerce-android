package com.woocommerce.android.ui.prefs

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_LOGOUT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTING_CHANGE
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_settings_main.*
import javax.inject.Inject

class MainSettingsFragment : Fragment(), MainSettingsContract.View {
    companion object {
        const val TAG = "main-settings"
        private const val SETTING_NOTIFS_ORDERS = "notifications_orders"
        private const val SETTING_NOTIFS_REVIEWS = "notifications_reviews"
        private const val SETTING_NOTIFS_TONE = "notifications_tone"

        fun newInstance(): MainSettingsFragment {
            return MainSettingsFragment()
        }
    }

    @Inject lateinit var presenter: MainSettingsContract.Presenter

    interface AppSettingsListener {
        fun onRequestLogout()
        fun onRequestShowPrivacySettings()
        fun onRequestShowAbout()
        fun onRequestShowLicenses()
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
            AnalyticsTracker.track(SETTINGS_LOGOUT_BUTTON_TAPPED)
            listener.onRequestLogout()
        }

        switchNotifsOrders.isChecked = AppPrefs.isOrderNotificationsEnabled()
        switchNotifsOrders.setOnCheckedChangeListener { _, isChecked ->
            doSettingToggled(SETTING_NOTIFS_ORDERS, isChecked)
            AppPrefs.setOrderNotificationsEnabled(isChecked)
            updateNotificationSettings()
        }

        switchNotifsReviews.isChecked = AppPrefs.isReviewNotificationsEnabled()
        switchNotifsReviews.setOnCheckedChangeListener { _, isChecked ->
            doSettingToggled(SETTING_NOTIFS_REVIEWS, isChecked)
            AppPrefs.setReviewNotificationsEnabled(isChecked)
        }

        switchNotifsTone.isChecked = AppPrefs.isOrderNotificationsChaChingEnabled()
        switchNotifsTone.setOnCheckedChangeListener { _, isChecked ->
            doSettingToggled(SETTING_NOTIFS_TONE, isChecked)
            AppPrefs.setOrderNotificationsChaChingEnabled(isChecked)
        }

        updateNotificationSettings()

        textPrivacySettings.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED)
            listener.onRequestShowPrivacySettings()
        }

        textAbout.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED)
            listener.onRequestShowAbout()
        }

        textLicenses.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED)
            listener.onRequestShowLicenses()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.settings)
    }

    /**
     * The review and tone notification switches are only enabled when order notifications are enabled
     */
    private fun updateNotificationSettings() {
        val enable = AppPrefs.isOrderNotificationsEnabled()
        switchNotifsReviews.isEnabled = enable
        switchNotifsTone.isEnabled = enable
    }

    private fun doSettingToggled(keyName: String, newValue: Boolean) {
        AnalyticsTracker.track(
                SETTING_CHANGE, mapOf(
                AnalyticsTracker.KEY_NAME to keyName,
                AnalyticsTracker.KEY_FROM to !newValue,
                AnalyticsTracker.KEY_TO to newValue))
    }
}
