package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.ActivityAppSettingsBinding
import com.woocommerce.android.push.NotificationMessageHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.PreferencesWrapper
import dagger.android.DispatchingAndroidInjector
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AppSettingsActivity :
    AppCompatActivity(),
    AppSettingsListener,
    AppSettingsContract.View {
    companion object {
        private const val KEY_SITE_CHANGED = "key_site_changed"
        const val RESULT_CODE_SITE_CHANGED = Activity.RESULT_FIRST_USER
        const val RESULT_CODE_BETA_OPTIONS_CHANGED = 2
        const val KEY_BETA_OPTION_CHANGED = "key_beta_option_changed"
    }

    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var presenter: AppSettingsContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var prefs: AppPrefs
    @Inject lateinit var notificationMessageHandler: NotificationMessageHandler

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private var siteChanged = false
    private var isBetaOptionChanged = false

    private lateinit var binding: ActivityAppSettingsBinding
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter.takeView(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        savedInstanceState?.let {
            siteChanged = it.getBoolean(KEY_SITE_CHANGED)
            isBetaOptionChanged = it.getBoolean(KEY_BETA_OPTION_CHANGED)
        }

        if (siteChanged) {
            setResult(RESULT_CODE_SITE_CHANGED)
        }
        if (isBetaOptionChanged) {
            setResult(RESULT_CODE_BETA_OPTIONS_CHANGED)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_SITE_CHANGED, siteChanged)
        outState.putBoolean(KEY_BETA_OPTION_CHANGED, isBetaOptionChanged)
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        AnalyticsTracker.trackBackPressed(this)
        return if (findNavController(R.id.nav_host_fragment).navigateUp()) {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_24dp)
            true
        } else {
            finish()
            true
        }
    }

    /**
     * User switched sites from the main settings fragment, set the result code so the calling activity
     * will know the site changed
     */
    override fun onSiteChanged() {
        if (FeatureFlag.CARD_READER.isEnabled()) presenter.clearCardReaderData()
        siteChanged = true
        setResult(RESULT_CODE_SITE_CHANGED)

        prefs.resetSitePreferences()
    }

    override fun onRequestLogout() {
        confirmLogout()
    }

    override fun onProductAddonsOptionChanged(enabled: Boolean) {
        if (AppPrefs.isProductAddonsEnabled != enabled) {
            isBetaOptionChanged = true
            AppPrefs.isProductAddonsEnabled = enabled
            setResult(RESULT_CODE_BETA_OPTIONS_CHANGED)
        }
    }

    override fun finishLogout() {
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(mainIntent)
        setResult(Activity.RESULT_OK)

        close()
    }

    override fun close() {
        finish()
    }

    override fun confirmLogout() {
        val message = String.format(
            Locale.getDefault(),
            getString(R.string.settings_confirm_logout),
            presenter.getAccountDisplayName()
        )
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton(R.string.signout) { _, _ ->
                AnalyticsTracker.track(
                    Stat.SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT,
                    mapOf(
                        AnalyticsTracker.KEY_RESULT to AnalyticsUtils.getConfirmationResultLabel(true)
                    )
                )

                if (FeatureFlag.CARD_READER.isEnabled()) presenter.clearCardReaderData()
                presenter.logout()
            }
            .setNegativeButton(R.string.back) { _, _ ->
                AnalyticsTracker.track(
                    Stat.SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT,
                    mapOf(
                        AnalyticsTracker.KEY_RESULT to AnalyticsUtils.getConfirmationResultLabel(false)
                    )
                )
            }
            .setCancelable(true)
            .create()
            .show()
    }

    override fun clearNotificationPreferences() {
        sharedPreferences.edit().remove(PreferencesWrapper.WPCOM_PUSH_DEVICE_TOKEN).apply()
    }
}
