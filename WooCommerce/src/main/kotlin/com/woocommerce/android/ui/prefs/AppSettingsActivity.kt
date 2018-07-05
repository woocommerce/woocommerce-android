package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.prefs.AppSettingsFragment.AppSettingsListener
import kotlinx.android.synthetic.main.activity_app_settings.*

class AppSettingsActivity : AppCompatActivity(), AppSettingsListener {
    companion object {
        const val KEY_LOGOUT_ON_RETURN = "logout_on_return"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            showAppSettingsFragment()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return false
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestLogout() {
        confirmLogout()
    }

    override fun onRequestPrivacySettings() {
        showPrivacySettingsFragment()
    }

    private fun showAppSettingsFragment() {
        val fragment = AppSettingsFragment.newInstance()
        showFragment(fragment, AppSettingsFragment.TAG, false)
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_SETTINGS)
    }

    private fun showPrivacySettingsFragment() {
        val fragment = PrivacySettingsFragment.newInstance()
        showFragment(fragment, PrivacySettingsFragment.TAG, true)
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_PRIVACY_SETTINGS)
    }

    private fun confirmLogout() {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.Woo_Dialog))
                .setMessage(R.string.settings_confirm_signout)
                .setPositiveButton(R.string.signout) { dialog, whichButton -> logout() }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .create()
                .show()
    }

    private fun logout() {
        // the actual logout will be handled by the main activity
        val data = Intent()
        data.putExtra(KEY_LOGOUT_ON_RETURN, true)
        setResult(Activity.RESULT_OK, data)
        finish()
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
