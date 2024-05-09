package com.woocommerce.android.ui.prefs

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_BETA_FEATURES_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_DOMAINS_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_FEATURE_REQUEST_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_IMAGE_OPTIMIZATION_TOGGLED
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_LOGOUT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.SETTINGS_WE_ARE_HIRING_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentSettingsMainBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.AppThemeUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.ThemeOption
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainSettingsFragment : Fragment(R.layout.fragment_settings_main), MainSettingsContract.View {
    companion object {
        const val TAG = "main-settings"
    }

    @Inject
    lateinit var presenter: MainSettingsContract.Presenter

    private var _binding: FragmentSettingsMainBinding? = null
    private val binding get() = _binding!!

    interface AppSettingsListener {
        fun onRequestLogout()
        fun onProductAddonsOptionChanged(enabled: Boolean)
    }

    private lateinit var settingsListener: AppSettingsListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsMainBinding.inflate(inflater, container, false)

        return binding.root
    }

    @Suppress("ForbiddenComment", "LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsMainBinding.bind(view)

        presenter.takeView(this)

        if (activity is AppSettingsListener) {
            settingsListener = activity as AppSettingsListener
        } else {
            throw ClassCastException(context.toString() + " must implement AppSettingsListener")
        }

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

        binding.optionHelpAndSupport.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.MAIN_MENU_CONTACT_SUPPORT_TAPPED)
            startActivity(HelpActivity.createIntent(requireActivity(), HelpOrigin.SETTINGS, null))
        }

        binding.optionNotifications.optionTitle = if (presenter.isChaChingSoundEnabled) {
            getString(R.string.settings_notifs_device)
        } else {
            getString(R.string.settings_notifs)
        }
        binding.optionNotifications.optionValue = if (presenter.isChaChingSoundEnabled) {
            getString(R.string.settings_notifs_device_detail)
        } else {
            null
        }
        binding.optionNotifications.setOnClickListener {
            presenter.onNotificationsClicked()
        }

        if (PackageUtils.isDebugBuild()) {
            binding.optionDevelopers.visibility = View.VISIBLE
            binding.optionDevelopers.setOnClickListener {
                findNavController().navigateSafely(R.id.action_mainSettingsFragment_to_developerOptionsFragment)
            }
        } else {
            binding.optionDevelopers.visibility = View.GONE
        }

        binding.optionBetaFeatures.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_BETA_FEATURES_BUTTON_TAPPED)
            val action = MainSettingsFragmentDirections.actionMainSettingsFragmentToBetaFeaturesFragment()
            findNavController().navigateSafely(action)
        }

        binding.optionPrivacy.setOnClickListener {
            AnalyticsTracker.track(SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED)
            findNavController().navigateSafely(
                MainSettingsFragmentDirections.actionMainSettingsFragmentToPrivacySettingsFragment(
                    RequestedAnalyticsValue.NONE
                )
            )
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

        binding.optionTheme.optionValue = getString(AppPrefs.getAppTheme().label)
        binding.optionTheme.setOnClickListener {
            showThemeChooser()
        }

        lifecycleScope.launch {
            binding.optionDomain.isVisible = presenter.isDomainOptionVisible
            binding.optionDomain.setOnClickListener {
                AnalyticsTracker.track(SETTINGS_DOMAINS_TAPPED)
                showDomainDashboard()
            }
        }

        binding.optionAccountSettings.isVisible = presenter.isCloseAccountOptionVisible
        binding.optionAccountSettings.setOnClickListener {
            findNavController().navigateSafely(
                MainSettingsFragmentDirections.actionMainSettingsFragmentToAccountSettingsFragment()
            )
        }

        presenter.setupAnnouncementOption()
        presenter.setupJetpackInstallOption()
        presenter.setupApplicationPasswordsSettings()

        binding.storeSettingsContainer.isVisible = binding.optionInstallJetpack.isVisible ||
            binding.optionDomain.isVisible ||
            binding.optionStoreName.isVisible

        binding.optionStoreName.setOnClickListener {
            findNavController()
                .navigateSafely(
                    MainSettingsFragmentDirections.actionMainSettingsFragmentToNameYourStoreDialogFragment()
                )
        }

        binding.optionSiteThemes.isVisible = presenter.isThemePickerOptionVisible
        binding.optionSiteThemes.setOnClickListener {
            findNavController()
                .navigateSafely(
                    MainSettingsFragmentDirections.actionMainSettingsFragmentToThemePickerFragment()
                )
        }

        binding.optionSitePlugins.setOnClickListener {
            findNavController()
                .navigateSafely(
                    MainSettingsFragmentDirections.actionMainSettingsFragmentToPluginsFragment()
                )
        }

        binding.wooPluginVersion.text = presenter.wooPluginVersion
    }

    private fun showDomainDashboard() {
        findNavController()
            .navigateSafely(MainSettingsFragmentDirections.actionMainSettingsFragmentToNavGraphDomainChange())
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

    /**
     * Shows the device's notification settings for this app - only implemented for API 26+ since we only call
     * this on API 26+ devices (will do nothing on older devices)
     */
    override fun showDeviceAppNotificationSettings() {
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("android.provider.extra.APP_PACKAGE", activity?.packageName)
        activity?.startActivity(intent)
    }

    override fun showNotificationsSettingsScreen() {
        findNavController().navigateSafely(
            MainSettingsFragmentDirections.actionMainSettingsFragmentToNotificationSettingsFragment()
        )
    }

    override fun handleJetpackInstallOption(supportsJetpackInstallation: Boolean) {
        if (supportsJetpackInstallation) {
            binding.optionInstallJetpack.visibility = View.VISIBLE
            binding.optionInstallJetpack.setOnClickListener {
                findNavController().navigateSafely(
                    MainSettingsFragmentDirections.actionMainSettingsFragmentToNavGraphJetpackInstall()
                )
            }
        }
    }

    override fun showLatestAnnouncementOption(announcement: FeatureAnnouncement) {
        binding.optionWhatsNew.show()
        binding.optionWhatsNew.setOnClickListener {
            WooLog.i(T.DEVICE, "Displaying Feature Announcement from Settings menu.")
            AnalyticsTracker.track(
                AnalyticsEvent.FEATURE_ANNOUNCEMENT_SHOWN,
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

    override fun handleApplicationPasswordsSettings() {
        binding.optionNotifications.hide()
    }

    private fun updateStoreSettings() {
        generateBetaFeaturesTitleList()
            .joinToString(", ")
            .takeIf { it.isNotEmpty() }
            ?.let { binding.optionBetaFeatures.optionValue = it }
    }

    private fun generateBetaFeaturesTitleList() =
        mutableListOf<String>().apply {
            add(getString(R.string.beta_features_add_ons))
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
