package com.woocommerce.android.baselineprofile

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.quicklogin.QuickLoginHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalBaselineProfilesApi
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    private val helper = QuickLoginHelper(PACKAGE_NAME)

    @Test
    fun loggedInStartup() =
        baselineProfileRule.collectBaselineProfile(
            packageName = PACKAGE_NAME,
            profileBlock = {
                if (!helper.isLoggedIn()) {
                    helper.loginWithWordpress(
                        email = BuildConfig.QUICK_LOGIN_WP_EMAIL,
                        password = BuildConfig.QUICK_LOGIN_WP_PASSWORD,
                        webSite = BuildConfig.QUICK_LOGIN_WP_SITE,
                    )
                }
                pressHome()
                startActivityAndWait()
            }
        )
}
