package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_BETA_FEATURES_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_FEATURE_REQUEST_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_IMAGE_OPTIMIZATION_TOGGLED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_LOGOUT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_SELECTED_SITE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_WE_ARE_HIRING_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTING_CHANGE
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.widgets.WCPromoTooltip
import com.woocommerce.android.widgets.WCPromoTooltip.Feature
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_settings_main.*
import javax.inject.Inject

class MainSettingsFragment : androidx.fragment.app.Fragment(), MainSettingsContract.View {
    companion object {
        const val TAG = "main-settings"
        private const val SETTING_NOTIFS_ORDERS = "notifications_orders"
        private const val SETTING_NOTIFS_REVIEWS = "notifications_reviews"
        private const val SETTING_NOTIFS_TONE = "notifications_tone"
    }

    @Inject lateinit var presenter: MainSettingsContract.Presenter

    interface AppSettingsListener {
        fun onRequestLogout()
        fun onSiteChanged()
        fun onV4StatsOptionChanged(enabled: Boolean)
        fun onProductsFeatureOptionChanged(enabled: Boolean)
    }

    private lateinit var settingsListener: AppSettingsListener

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is AppSettingsListener) {
            settingsListener = activity as AppSettingsListener
        } else {
            throw ClassCastException(context.toString() + " must implement AppSettingsListener")
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        updateStoreViews()

        buttonLogout.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_LOGOUT_BUTTON_TAPPED)
            settingsListener.onRequestLogout()
        }

        with(settingsHiring) {
            val hiringText = getString(R.string.settings_hiring)
            val settingsFooterText = getString(R.string.settings_footer, hiringText)
            val spannable = SpannableString(settingsFooterText)
            spannable.setSpan(
                    WooClickableSpan {
                        AnalyticsTracker.track(SETTINGS_WE_ARE_HIRING_BUTTON_TAPPED)
                        ChromeCustomTabUtils.launchUrl(context, AppUrls.AUTOMATTIC_HIRING)
                    },
                    (settingsFooterText.length - hiringText.length),
                    settingsFooterText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            setText(spannable, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethod.getInstance()
            setLinkTextColor(ContextCompat.getColor(context, R.color.wc_purple))
        }

        if (FeatureFlag.PRODUCT_RELEASE_M2.isEnabled(requireActivity())) {
            switchImageOptimizaton.visibility = View.VISIBLE
            switchImageOptimizaton.isChecked = AppPrefs.getImageOptimizationEnabled()
            switchImageOptimizaton.setOnCheckedChangeListener { _, isChecked ->
                AnalyticsTracker.track(
                        SETTINGS_IMAGE_OPTIMIZATION_TOGGLED,
                        mapOf(AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(isChecked))
                )
                AppPrefs.setImageOptimizationEnabled(isChecked)
            }
        } else {
            switchImageOptimizaton.visibility = View.GONE
        }

        // on API 26+ we show the device notification settings, on older devices we have in-app settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifsContainerOlder.visibility = View.GONE
            notifsContainerNewer.visibility = View.VISIBLE
            notifsContainerNewer.setOnClickListener {
                AnalyticsTracker.track(SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED)
                showDeviceAppNotificationSettings()
            }
        } else {
            notifsContainerOlder.visibility = View.VISIBLE
            notifsContainerNewer.visibility = View.GONE

            switchNotifsOrders.isChecked = AppPrefs.isOrderNotificationsEnabled()
            switchNotifsOrders.setOnCheckedChangeListener { _, isChecked ->
                trackSettingToggled(SETTING_NOTIFS_ORDERS, isChecked)
                AppPrefs.setOrderNotificationsEnabled(isChecked)
                switchNotifsTone.isEnabled = isChecked
            }

            switchNotifsReviews.isChecked = AppPrefs.isReviewNotificationsEnabled()
            switchNotifsReviews.setOnCheckedChangeListener { _, isChecked ->
                trackSettingToggled(SETTING_NOTIFS_REVIEWS, isChecked)
                AppPrefs.setReviewNotificationsEnabled(isChecked)
            }

            switchNotifsTone.isChecked = AppPrefs.isOrderNotificationsChaChingEnabled()
            switchNotifsTone.isEnabled = AppPrefs.isOrderNotificationsEnabled()
            switchNotifsTone.setOnCheckedChangeListener { _, isChecked ->
                trackSettingToggled(SETTING_NOTIFS_TONE, isChecked)
                AppPrefs.setOrderNotificationsChaChingEnabled(isChecked)
            }
        }

        betaFeaturesContainer.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_BETA_FEATURES_BUTTON_TAPPED)
            findNavController().navigate(R.id.action_mainSettingsFragment_to_betaFeaturesFragment)
        }

        // TODO: commenting out this code since we are enabling products for all users in the next release.
        // Once product M2 editing features are live, we can enable product editing switch from this screen again.
        textBetaFeaturesDetail.text = getString(R.string.settings_enable_v4_stats_title)

//         // if v4 stats is available, show both products & stats under the beta setting label, otherwise
//         // only show products
//        textBetaFeaturesDetail.text = if (AppPrefs.isUsingV4Api()) {
//            getString(R.string.settings_enable_product_teaser_title) +
//                    ", " +
//                    getString(R.string.settings_enable_v4_stats_title)
//        } else {
//            getString(R.string.settings_enable_product_teaser_title)
//        }

        textPrivacySettings.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED)
            findNavController().navigate(R.id.action_mainSettingsFragment_to_privacySettingsFragment)
        }

        textFeatureRequests.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_FEATURE_REQUEST_BUTTON_TAPPED)
            context?.let { ChromeCustomTabUtils.launchUrl(it, AppUrls.APP_FEATURE_REQUEST) }
        }

        textAbout.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED)
            findNavController().navigate(R.id.action_mainSettingsFragment_to_aboutFragment)
        }

        textLicenses.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED)
            findNavController().navigate(R.id.action_mainSettingsFragment_to_licensesFragment)
        }

        if (presenter.hasMultipleStores()) {
            primaryStoreView.setOnClickListener {
                AnalyticsTracker.track(SETTINGS_SELECTED_SITE_TAPPED)
                SitePickerActivity.showSitePickerForResult(this)
            }

            // advertise the site switcher if we haven't already
            WCPromoTooltip.showIfNeeded(Feature.SITE_SWITCHER, primaryStoreView)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.settings)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // if we're returning from the site picker, make sure the new store is shown and the activity
        // knows it has changed
        if (requestCode == RequestCodes.SITE_PICKER && resultCode == Activity.RESULT_OK) {
            updateStoreViews()
            settingsListener.onSiteChanged()
        }
    }

    /**
     * Shows the device's notification settings for this app - only implemented for API 26+ since we only call
     * this on API 26+ devices (will do nothing on older devices)
     */
    override fun showDeviceAppNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("android.provider.extra.APP_PACKAGE", activity?.packageName)
            activity?.startActivity(intent)
        }
    }

    private fun updateStoreViews() {
        textPrimaryStoreDomain.text = presenter.getStoreDomainName()
        textPrimaryStoreUsername.text = presenter.getUserDisplayName()
    }

    /**
     * Called when a boolean setting is changed so we can track it
     */
    private fun trackSettingToggled(keyName: String, newValue: Boolean) {
        AnalyticsTracker.track(
                SETTING_CHANGE, mapOf(
                AnalyticsTracker.KEY_NAME to keyName,
                AnalyticsTracker.KEY_FROM to !newValue,
                AnalyticsTracker.KEY_TO to newValue)
        )
    }
}
