package com.woocommerce.android.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.main.MainActivity

class MagicLinkInterceptActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPENED)

        val intent = Intent(this, MainActivity::class.java)
        intent.action = getIntent().action
        intent.data = getIntent().data
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
