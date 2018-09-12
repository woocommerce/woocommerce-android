package com.woocommerce.android.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.ui.main.MainActivity
import dagger.android.AndroidInjection
import org.wordpress.android.login.LoginAnalyticsListener
import javax.inject.Inject

class MagicLinkInterceptActivity : Activity() {
    @Inject internal lateinit var loginAnalyticsListener: LoginAnalyticsListener

    private var isUpEvent = false // Tracks if the user clicked the up menu or device back button

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        loginAnalyticsListener.trackLoginMagicLinkOpened()

        val intent = Intent(this, MainActivity::class.java)
        intent.action = getIntent().action
        intent.data = getIntent().data
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (!isUpEvent) {
            isUpEvent = false
            AnalyticsTracker.track(Stat.DEVICE_BACK_BUTTON_TAPPED)
        }
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return item?.itemId?.let {
            AnalyticsTracker.track(Stat.MAIN_MENU_UP_TAPPED)
            isUpEvent = true
            onBackPressed()
            true
        } ?: super.onOptionsItemSelected(item)
    }
}
