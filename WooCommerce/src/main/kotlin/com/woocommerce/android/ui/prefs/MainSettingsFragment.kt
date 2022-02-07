package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.content.Intent
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
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.*
import com.woocommerce.android.databinding.FragmentSettingsMainBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.util.*
import com.woocommerce.android.util.FeatureFlag.CARD_READER
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
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
        fun onProductAddonsOptionChanged(enabled: Boolean)
        fun onSimplePaymentsOptionChanged(enabled: Boolean)
        fun onOrderCreationOptionChanged(enabled: Boolean)
    }

    private lateinit var settingsListener: AppSettingsListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSettingsMainBinding.bind(view)

        presenter.takeView(this)

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
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

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

        updateStoreSettings()
        binding.optionCardReaderPayments.setOnClickListener {
            findNavController().navigateSafely(R.id.action_mainSettingsFragment_to_cardReaderOnboardingFragment)
        }

        binding.optionHelpAndSupport.setOnClickListener {
            AnalyticsTracker.track(Stat.MAIN_MENU_CONTACT_SUPPORT_TAPPED)
            startActivity(HelpActivity.createIntent(requireActivity(), Origin.SETTINGS, null))
        }

        // on API 26+ we show the device notification settings, on older devices we have in-app settings
        if (SystemVersionUtils.isAtLeastO()) {
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
            val action = MainSettingsFragmentDirections.actionMainSettingsFragmentToBetaFeaturesFragment(
                isCardReaderOnboardingCompleted = presenter.isCardReaderOnboardingCompleted()
            )
            findNavController().navigateSafely(action)
        }

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
            findNavController().navigateSafely(R.id.action_mainSettingsFragment_to_aboutActivity)
        }

        binding.optionLicenses.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED)
            findNavController().navigateSafely(R.id.action_mainSettingsFragment_to_licensesFragment)
        }

        if (presenter.hasMultipleStores()) {
            val storeClickListener = View.OnClickListener {
                AnalyticsTracker.track(SETTINGS_SELECTED_SITE_TAPPED)
                SitePickerActivity.showSitePickerForResult(this)
            }
            binding.optionStore.setOnClickListener(storeClickListener)
            binding.optionSwitchStore.setOnClickListener(storeClickListener)
        } else {
            binding.optionSwitchStore.hide()
        }

        binding.optionTheme.optionValue = getString(AppPrefs.getAppTheme().label)
        binding.optionTheme.setOnClickListener {
            // FIXME AMANDA tracks event
            showThemeChooser()
        }

        presenter.setupAnnouncementOption()
        presenter.setupJetpackInstallOption()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        activity?.setTitle(R.string.settings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        presenter.dropView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // if we're returning from the site picker, make sure the new store is shown and the activity
        // knows it has changed
        if (requestCode == RequestCodes.SITE_PICKER && resultCode == Activity.RESULT_OK) {
            updateStoreViews()
            updateStoreSettings()
            settingsListener.onSiteChanged()
            presenter.setupJetpackInstallOption()
        }
    }

    /**
     * Shows the device's notification settings for this app - only implemented for API 26+ since we only call
     * this on API 26+ devices (will do nothing on older devices)
     */
    override fun showDeviceAppNotificationSettings() {
        if (SystemVersionUtils.isAtLeastO()) {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("android.provider.extra.APP_PACKAGE", activity?.packageName)
            activity?.startActivity(intent)
        }
    }

    override fun handleJetpackInstallOption(isJetpackCPSite: Boolean) {
        if (isJetpackCPSite) {
            binding.optionInstallJetpack.visibility = View.VISIBLE
            binding.optionInstallJetpack.setOnClickListener {
                findNavController().navigateSafely(
                    MainSettingsFragmentDirections.actionMainSettingsFragmentToNavGraphJetpackInstall()
                )
            }
        } else {
            binding.optionInstallJetpack.visibility = View.GONE
        }
    }

    override fun showLatestAnnouncementOption(announcement: FeatureAnnouncement) {
        binding.optionWhatsNew.show()
        binding.optionWhatsNew.setOnClickListener {
            WooLog.i(T.DEVICE, "Displaying Feature Announcement from Settings menu.")
            AnalyticsTracker.track(
                Stat.FEATURE_ANNOUNCEMENT_SHOWN,
                mapOf(
                    AnalyticsTracker.KEY_ANNOUNCEMENT_VIEW_SOURCE to
                        AnalyticsTracker.VALUE_ANNOUNCEMENT_SOURCE_SETTINGS
                )
            )
            findNavController()
                .navigateSafely(
                    MainSettingsFragmentDirections.actionMainSettingsFragmentToFeatureAnnouncementDialogFragment(
                        announcement
                    )
                )
        }
    }

    private fun updateStoreViews() {
        binding.optionStore.optionTitle = presenter.getStoreDomainName()
        binding.optionStore.optionValue = presenter.getUserDisplayName()
    }

    private fun updateStoreSettings() {
        binding.storeSettingsContainer.visibility =
            if (CARD_READER.isEnabled()) View.VISIBLE else View.GONE

        generateBetaFeaturesTitleList()
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
            ?.let { binding.optionBetaFeatures.optionValue = it }
    }

    private fun generateBetaFeaturesTitleList() =
        mutableListOf<String>().apply {
            add(getString(R.string.beta_features_add_ons))
            add(getString(R.string.beta_features_order_creation))
            if (presenter.isCardReaderOnboardingCompleted()) {
                add(getString(R.string.beta_features_simple_payments))
            }
        }

    /**
     * Called when a boolean setting is changed so we can track it
     */
    private fun trackSettingToggled(keyName: String, newValue: Boolean) {
        AnalyticsTracker.track(
            SETTING_CHANGE,
            mapOf(
                AnalyticsTracker.KEY_NAME to keyName,
                AnalyticsTracker.KEY_FROM to !newValue,
                AnalyticsTracker.KEY_TO to newValue
            )
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
