package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
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
import com.woocommerce.android.databinding.FragmentSettingsMainBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.AppThemeUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.ThemeOption
import com.woocommerce.android.widgets.WCPromoTooltip
import com.woocommerce.android.widgets.WCPromoTooltip.Feature
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class MainSettingsFragment : Fragment(R.layout.fragment_settings_main), MainSettingsContract.View {
    companion object {
        const val TAG = "main-settings"
        private const val SETTING_NOTIFS_ORDERS = "notifications_orders"
        private const val SETTING_NOTIFS_REVIEWS = "notifications_reviews"
        private const val SETTING_NOTIFS_TONE = "notifications_tone"
    }

    @Inject lateinit var presenter: MainSettingsContract.Presenter

    private var _binding: FragmentSettingsMainBinding? = null
    private val binding get() = _binding!!

    interface AppSettingsListener {
        fun onRequestLogout()
        fun onSiteChanged()
        fun onProductsFeatureOptionChanged(enabled: Boolean)
    }

    private lateinit var settingsListener: AppSettingsListener

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSettingsMainBinding.bind(view)

        if (activity is AppSettingsListener) {
            settingsListener = activity as AppSettingsListener
        } else {
            throw ClassCastException(context.toString() + " must implement AppSettingsListener")
        }

        updateStoreViews()

        binding.btnOptionLogout.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_LOGOUT_BUTTON_TAPPED)
            settingsListener.onRequestLogout()
        }

        with(binding.settingsHiring) {
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
        }

        binding.optionImageOptimization.visibility = View.VISIBLE
        binding.optionImageOptimization.isChecked = AppPrefs.getImageOptimizationEnabled()
        binding.optionImageOptimization.setOnCheckedChangeListener { _, isChecked ->
            AnalyticsTracker.track(
                    SETTINGS_IMAGE_OPTIMIZATION_TOGGLED,
                    mapOf(AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(isChecked))
            )
            AppPrefs.setImageOptimizationEnabled(isChecked)
        }

        binding.optionHelpAndSupport.setOnClickListener {
            AnalyticsTracker.track(Stat.MAIN_MENU_CONTACT_SUPPORT_TAPPED)
            startActivity(HelpActivity.createIntent(requireActivity(), Origin.SETTINGS, null))
        }

        // on API 26+ we show the device notification settings, on older devices we have in-app settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.containerNotifsOld.visibility = View.GONE
            binding.containerNotifsNew.visibility = View.VISIBLE
            binding.optionNotifications.setOnClickListener {
                AnalyticsTracker.track(SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED)
                showDeviceAppNotificationSettings()
            }
        } else {
            binding.containerNotifsOld.visibility = View.VISIBLE
            binding.containerNotifsNew.visibility = View.GONE

            binding.optionNotifsOrders.isChecked = AppPrefs.isOrderNotificationsEnabled()
            binding.optionNotifsOrders.setOnCheckedChangeListener { _, isChecked ->
                trackSettingToggled(SETTING_NOTIFS_ORDERS, isChecked)
                AppPrefs.setOrderNotificationsEnabled(isChecked)
                binding.optionNotifsTone.isEnabled = isChecked
            }

            binding.optionNotifsReviews.isChecked = AppPrefs.isReviewNotificationsEnabled()
            binding.optionNotifsReviews.setOnCheckedChangeListener { _, isChecked ->
                trackSettingToggled(SETTING_NOTIFS_REVIEWS, isChecked)
                AppPrefs.setReviewNotificationsEnabled(isChecked)
            }

            binding.optionNotifsTone.isChecked = AppPrefs.isOrderNotificationsChaChingEnabled()
            binding.optionNotifsTone.isEnabled = AppPrefs.isOrderNotificationsEnabled()
            binding.optionNotifsTone.setOnCheckedChangeListener { _, isChecked ->
                trackSettingToggled(SETTING_NOTIFS_TONE, isChecked)
                AppPrefs.setOrderNotificationsChaChingEnabled(isChecked)
            }
        }

        binding.optionBetaFeatures.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_BETA_FEATURES_BUTTON_TAPPED)
            findNavController().navigateSafely(R.id.action_mainSettingsFragment_to_betaFeaturesFragment)
        }

        binding.optionBetaFeatures.optionValue = getString(R.string.settings_enable_product_adding_teaser_title)

        // No beta features currently available
        binding.optionBetaFeatures.hide()

        binding.optionPrivacy.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED)
            findNavController().navigateSafely(R.id.action_mainSettingsFragment_to_privacySettingsFragment)
        }

        binding.optionSendFeedback.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_FEATURE_REQUEST_BUTTON_TAPPED)
            findNavController().navigateSafely(R.id.action_mainSettingsFragment_feedbackSurveyFragment)
        }

        binding.optionAbout.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED)
            findNavController().navigateSafely(R.id.action_mainSettingsFragment_to_aboutFragment)
        }

        binding.optionLicenses.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED)
            findNavController().navigateSafely(R.id.action_mainSettingsFragment_to_licensesFragment)
        }

        if (presenter.hasMultipleStores()) {
            binding.optionStore.setOnClickListener {
                AnalyticsTracker.track(SETTINGS_SELECTED_SITE_TAPPED)
                SitePickerActivity.showSitePickerForResult(this)
            }

            // advertise the site switcher if we haven't already
            WCPromoTooltip.showIfNeeded(Feature.SITE_SWITCHER, binding.optionStore)
        }

        binding.optionTheme.optionValue = getString(AppPrefs.getAppTheme().label)
        binding.optionTheme.setOnClickListener {
            // FIXME AMANDA tracks event
            showThemeChooser()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.settings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        binding.optionStore.optionTitle = presenter.getStoreDomainName()
        binding.optionStore.optionValue = presenter.getUserDisplayName()
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

    private fun showThemeChooser() {
        val currentTheme = AppPrefs.getAppTheme()
        val valuesArray = ThemeOption.values().map { getString(it.label) }.toTypedArray()
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.settings_app_theme_title))
                .setSingleChoiceItems(valuesArray, currentTheme.ordinal) { dialog, which ->
                    val selectedTheme = ThemeOption.values()[which]
                    AppThemeUtils.setAppTheme(selectedTheme)
                    binding.optionTheme.optionValue = getString(selectedTheme.label)
                    dialog.dismiss()
                }
                .show()
    }
}
