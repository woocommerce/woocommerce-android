package com.woocommerce.android.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import dagger.android.AndroidInjection
import org.wordpress.android.login.LoginAnalyticsListener
import javax.inject.Inject

class MagicLinkInterceptActivity : Activity() {
    @Inject internal lateinit var loginAnalyticsListener: LoginAnalyticsListener

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        loginAnalyticsListener.trackLoginMagicLinkOpened()

        val intent = Intent(this, LoginActivity::class.java)
        intent.action = getIntent().action
        intent.data = getIntent().data
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
