package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.Toolbar
import com.woocommerce.android.R
import com.woocommerce.android.ui.prefs.AppSettingsFragment.AppSettingsListener
import kotlinx.android.synthetic.main.activity_app_settings.*
import org.wordpress.android.util.AppLog

class AppSettingsActivity : AppCompatActivity(), AppSettingsListener {
    companion object {
        const val KEY_LOGOUT = "logout"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar as Toolbar)

        if (savedInstanceState == null) {
            showAppSettingsFragment()
        }
    }

    override fun onRequestLogout() {
        confirmLogout()
    }

    override fun onRequestShowPrivacySettings() {
        showPrivacySettingsFragment()
    }

    override fun onRequestShowDeviceSettings() {
        showDeviceSettings()
    }

    private fun showAppSettingsFragment() {
        val fragment = AppSettingsFragment.newInstance()
        showFragment(fragment, PrivacySettingsFragment.TAG, false)
    }

    private fun showPrivacySettingsFragment() {
        val fragment = PrivacySettingsFragment.newInstance()
        showFragment(fragment, PrivacySettingsFragment.TAG, true)
    }

    private fun confirmLogout() {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.Woo_Dialog))
                .setMessage(R.string.settings_confirm_signout)
                .setTitle(R.string.signout)
                .setPositiveButton(R.string.signout) { dialog, whichButton -> logout() }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .create().show()
    }

    private fun logout() {
        // the actual logout will be handled by the main activity
        val data = Intent()
        data.putExtra(KEY_LOGOUT, true)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun showDeviceSettings() {
        try {
            // open specific app info screen
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            AppLog.w(AppLog.T.SETTINGS, exception.message)
            // open generic apps screen
            val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            startActivity(intent)
        }
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

