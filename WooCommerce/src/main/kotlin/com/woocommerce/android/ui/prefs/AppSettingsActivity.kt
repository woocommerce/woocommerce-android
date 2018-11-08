package com.woocommerce.android.ui.prefs

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.util.AnalyticsUtils
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_app_settings.*
import javax.inject.Inject

class AppSettingsActivity : AppCompatActivity(),
        AppSettingsListener,
        AppSettingsContract.View,
        HasSupportFragmentInjector {
    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var presenter: AppSettingsContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_settings)
        presenter.takeView(this)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            showAppSettingsFragment()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return false
    }

    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_white_24dp)
        }
    }

    override fun onRequestLogout() {
        confirmLogout()
    }

    override fun onRequestShowPrivacySettings() {
        showPrivacySettingsFragment()
    }

    override fun onRequestShowAboutScreen() {
        showAboutScreen()
    }

    override fun onRequestShowLicensesScreen() {
        showLicensesDialog()
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    override fun close() {
        finish()
    }

    override fun showAppSettingsFragment() {
        val fragment = MainSettingsFragment.newInstance()
        showFragment(fragment, MainSettingsFragment.TAG, false)
    }

    override fun showPrivacySettingsFragment() {
        val fragment = PrivacySettingsFragment.newInstance()
        showFragment(fragment, PrivacySettingsFragment.TAG, true)
    }

    override fun showAboutScreen() {
        val fragment = AboutFragment.newInstance()
        showFragment(fragment, AboutFragment.TAG, true)
    }

    override fun showLicensesDialog() {
        LicensesDialogFragment.newInstance().show(supportFragmentManager, LicensesDialogFragment.TAG)
    }

    override fun confirmLogout() {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.Woo_Dialog))
                .setMessage(R.string.settings_confirm_signout)
                .setPositiveButton(R.string.signout) { _, _ ->
                    AnalyticsTracker.track(SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT, mapOf(
                            AnalyticsTracker.KEY_RESULT to AnalyticsUtils.getConfirmationResultLabel(true)))

                    presenter.logout()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    AnalyticsTracker.track(SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT, mapOf(
                            AnalyticsTracker.KEY_RESULT to AnalyticsUtils.getConfirmationResultLabel(false)))
                }
                .setCancelable(true)
                .create()
                .show()
    }

    private fun showFragment(fragment: Fragment, tag: String, animate: Boolean) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate) {
            fragmentTransaction.setCustomAnimations(
                    R.anim.activity_slide_in_from_right,
                    R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left,
                    R.anim.activity_slide_out_to_right)
        }
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }
}
