package com.woocommerce.android.ui.prefs

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.Toolbar
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.activity_app_settings.*
import org.wordpress.android.util.AppLog

class AppSettingsActivity : AppCompatActivity() {
    companion object {
        const val KEY_LOGOUT = "logout"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar as Toolbar)

        textDeviceSettings.setOnClickListener {
            showDeviceSettings()
        }

        textLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.Woo_Dialog))
                .setMessage(R.string.confirm_signout_message)
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
}

