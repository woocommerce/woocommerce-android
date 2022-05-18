package com.woocommerce.android.benchmark

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.quicklogin.QuickLoginHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE_NAME = "com.woocommerce.android"

private const val EMAIL = ""
private const val PASSWORD = ""
private const val WEB_SITE = ""

@ExperimentalBaselineProfilesApi
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    private val helper = QuickLoginHelper(PACKAGE_NAME)

    @Test
    fun startup() =
        baselineProfileRule.collectBaselineProfile(packageName = PACKAGE_NAME) {
            helper.loginWithWordpress(
                email = EMAIL,
                password = PASSWORD,
                webSite = WEB_SITE,
            )
            pressHome()
            startActivityAndWait()
        }
}
