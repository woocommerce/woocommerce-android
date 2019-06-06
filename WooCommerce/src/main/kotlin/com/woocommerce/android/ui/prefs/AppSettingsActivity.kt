package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_app_settings.*
import java.util.Locale
import javax.inject.Inject

class AppSettingsActivity : AppCompatActivity(),
        AppSettingsListener,
        AppSettingsContract.View,
        HasSupportFragmentInjector {
    companion object {
        private const val KEY_SITE_CHANGED = "key_site_changed"
        const val RESULT_CODE_SITE_CHANGED = Activity.RESULT_FIRST_USER
    }

    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<androidx.fragment.app.Fragment>
    @Inject lateinit var presenter: AppSettingsContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var navController: NavController

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private var siteChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_settings)
        navController = findNavController(R.id.nav_host_fragment)
        presenter.takeView(this)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState != null) {
            siteChanged = savedInstanceState.getBoolean(KEY_SITE_CHANGED)
            if (siteChanged) {
                setResult(RESULT_CODE_SITE_CHANGED)
            }
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
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        AnalyticsTracker.trackBackPressed(this)
        return if (navController.navigateUp()) {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_white_24dp)
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
        siteChanged = true
        setResult(RESULT_CODE_SITE_CHANGED)

        // Display a message to the user advising notifications will only be shown
        // for the current store.
        selectedSite.getIfExists()?.let {
            Snackbar.make(
                    main_content,
                    getString(R.string.settings_switch_site_notifs_msg, it.name),
                    Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onRequestLogout() {
        confirmLogout()
    }

    override fun supportFragmentInjector(): AndroidInjector<androidx.fragment.app.Fragment> = fragmentInjector

    override fun close() {
        finish()
    }

    override fun confirmLogout() {
        val message = String.format(
                Locale.getDefault(),
                getString(R.string.settings_confirm_logout),
                presenter.getAccountDisplayName()
        )
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme))
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
