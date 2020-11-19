package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.android.synthetic.main.activity_app_settings.*
import kotlinx.android.synthetic.main.view_toolbar.*
import java.util.Locale
import javax.inject.Inject

class AppSettingsActivity : AppCompatActivity(),
        AppSettingsListener,
        AppSettingsContract.View,
        HasAndroidInjector {
    companion object {
        private const val KEY_SITE_CHANGED = "key_site_changed"
        const val RESULT_CODE_SITE_CHANGED = Activity.RESULT_FIRST_USER
        const val RESULT_CODE_BETA_OPTIONS_CHANGED = 2
        const val KEY_BETA_OPTION_CHANGED = "key_beta_option_changed"
    }

    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var presenter: AppSettingsContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private var siteChanged = false
    private var isBetaOptionChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_settings)
        presenter.takeView(this)

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
    // BaseTransientBottomBar.LENGTH_LONG is pointing to Snackabr.LENGTH_LONG which confuses checkstyle
    @Suppress("WrongConstant")
    override fun onSiteChanged() {
        siteChanged = true
        setResult(RESULT_CODE_SITE_CHANGED)
        NotificationHandler.removeAllNotificationsFromSystemBar(this)

        // Display a message to the user advising notifications will only be shown
        // for the current store.
        selectedSite.getIfExists()?.let {
            Snackbar.make(
                    main_content,
                    getString(R.string.settings_switch_site_notifs_msg, it.name),
                    BaseTransientBottomBar.LENGTH_LONG
            ).show()
        }
    }

    override fun onRequestLogout() {
        confirmLogout()
    }

    override fun onProductsFeatureOptionChanged(enabled: Boolean) {
        val isProductsFeatureEnabled = AppPrefs.isProductsFeatureEnabled()
        if (isProductsFeatureEnabled != enabled) {
            isBetaOptionChanged = true
            AppPrefs.setIsProductsFeatureEnabled(enabled)
            setResult(RESULT_CODE_BETA_OPTIONS_CHANGED)
        }
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun finishLogout() {
        NotificationHandler.removeAllNotificationsFromSystemBar(this)

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
                    AnalyticsTracker.track(Stat.SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT, mapOf(
                            AnalyticsTracker.KEY_RESULT to AnalyticsUtils.getConfirmationResultLabel(true)))

                    presenter.logout()
                }
                .setNegativeButton(R.string.back) { _, _ ->
                    AnalyticsTracker.track(Stat.SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT, mapOf(
                            AnalyticsTracker.KEY_RESULT to AnalyticsUtils.getConfirmationResultLabel(false)))
                }
                .setCancelable(true)
                .create()
                .show()
    }

    override fun clearNotificationPreferences() {
        sharedPreferences.edit().remove(FCMRegistrationIntentService.WPCOM_PUSH_DEVICE_TOKEN).apply()
    }
}
