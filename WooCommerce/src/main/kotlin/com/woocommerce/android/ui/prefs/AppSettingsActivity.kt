package com.woocommerce.android.ui.prefs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.activity_app_settings.*
import org.wordpress.android.util.AppLog

class AppSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar as Toolbar)

        textDeviceSettings.setOnClickListener {
            showDeviceSettings()
        }
    }

    private fun showDeviceSettings() {
        try {
            // open specific app info screen
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:" + getPackageName())
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            AppLog.w(AppLog.T.SETTINGS, exception.message)
            // open generic apps screen
            val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            startActivity(intent)
        }
    }
}

