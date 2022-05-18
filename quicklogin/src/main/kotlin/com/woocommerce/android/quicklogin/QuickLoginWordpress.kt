package com.woocommerce.android.quicklogin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.AppPrefs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val DEBUG_PACKAGE_NAME = "com.woocommerce.android.dev"

@RunWith(AndroidJUnit4::class)
class QuickLoginWordpress {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val helper = QuickLoginHelper(DEBUG_PACKAGE_NAME)

    @Before
    fun init() {
        AppPrefs.init(context)
    }

    @Test
    fun loginWithWordpress() {
        verifyEmailAndPassword()
        helper.loginWithWordpress(
            email = BuildConfig.QUICK_LOGIN_WP_EMAIL,
            password = BuildConfig.QUICK_LOGIN_WP_PASSWORD,
            webSite = BuildConfig.QUICK_LOGIN_WP_SITE,
        )
    }

    private fun verifyEmailAndPassword() {
        if (BuildConfig.QUICK_LOGIN_WP_EMAIL.isBlank() ||
            BuildConfig.QUICK_LOGIN_WP_PASSWORD.isBlank()
        ) {
            throw IllegalStateException("WP Email or password is not set. Look into quicklogin/woo_login.sh-example")
        }
    }
}
