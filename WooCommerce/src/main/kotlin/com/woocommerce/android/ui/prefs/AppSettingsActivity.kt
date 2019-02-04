package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.ui.prefs.AppSettingsActivity.FragmentAnim.NONE
import com.woocommerce.android.ui.prefs.AppSettingsActivity.FragmentAnim.SLIDE_IN
import com.woocommerce.android.ui.prefs.AppSettingsActivity.FragmentAnim.SLIDE_UP
import com.woocommerce.android.ui.prefs.MainSettingsFragment.AppSettingsListener
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
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
        private const val SITE_PICKER_REQUEST_CODE = 1000
    }

    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var presenter: AppSettingsContract.Presenter

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    enum class FragmentAnim {
        SLIDE_IN,
        SLIDE_UP,
        NONE
    }

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
            supportActionBar?.elevation = resources.getDimensionPixelSize(R.dimen.appbar_elevation).toFloat()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // if we're returning from the site picker, update the main fragment so the new store is shown
        if (requestCode == SITE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            supportFragmentManager.findFragmentByTag(MainSettingsFragment.TAG)?.let {
                (it as MainSettingsFragment).updateStoreViews()
            }
        }
    }

    override fun onRequestLogout() {
        confirmLogout()
    }

    override fun onRequestShowPrivacySettings() {
        showPrivacySettingsFragment()
    }

    override fun onRequestShowAbout() {
        showAboutFragment()
    }

    override fun onRequestShowLicenses() {
        showLicensesFragment()
    }

    override fun onRequestShowSitePicker() {
        SitePickerActivity.showSitePickerForResult(this, SITE_PICKER_REQUEST_CODE)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    override fun close() {
        finish()
    }

    override fun showAppSettingsFragment() {
        val fragment = MainSettingsFragment.newInstance()
        showFragment(fragment, MainSettingsFragment.TAG, NONE)
    }

    override fun showPrivacySettingsFragment() {
        val fragment = PrivacySettingsFragment.newInstance()
        showFragment(fragment, PrivacySettingsFragment.TAG)
    }

    override fun showAboutFragment() {
        val fragment = AboutFragment.newInstance()
        showFragment(fragment, AboutFragment.TAG, SLIDE_UP)
    }

    override fun showLicensesFragment() {
        val fragment = LicensesFragment.newInstance()
        showFragment(fragment, LicensesFragment.TAG, SLIDE_UP)
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

    private fun showFragment(fragment: Fragment, tag: String, anim: FragmentAnim = SLIDE_IN) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (anim == SLIDE_IN) {
            fragmentTransaction.setCustomAnimations(
                    R.anim.activity_slide_in_from_right,
                    R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left,
                    R.anim.activity_slide_out_to_right)
        } else if (anim == SLIDE_UP) {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_up,
                    0,
                    0,
                    R.anim.slide_out_down)
        }
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }
}
