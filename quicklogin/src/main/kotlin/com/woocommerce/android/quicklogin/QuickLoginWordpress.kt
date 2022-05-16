package com.woocommerce.android.quicklogin

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction.DOWN
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.woocommerce.android.AppPrefs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val DEBUG_PACKAGE_NAME = "com.woocommerce.android.dev"
private const val SHORT_TIMEOUT = 2000L
private const val TIMEOUT = 5000L
private const val LONG_TIMEOUT = 60000L

private const val SECOND_FACTOR_LENGTH = 6
private const val SITE_FLINGS_COUNT = 10

@RunWith(AndroidJUnit4::class)
class QuickLoginWordpress {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun init() {
        AppPrefs.init(context)
    }

    @Test
    fun loginWithWordpress() {
        verifyEmailAndPassword()
        startTheApp()
        chooseWpComLogin()
        enterEmail()
        enterPassword()
        enterSecondFactorIfNeeded()
        selectSiteIfProvided()
    }

    private fun verifyEmailAndPassword() {
        if (BuildConfig.QUICK_LOGIN_WP_EMAIL.isBlank() ||
            BuildConfig.QUICK_LOGIN_WP_PASSWORD.isBlank()
        ) {
            exitFlowWithMessage("WP Email or password is not set. Look into quicklogin/woo_login.sh-example")
        }
    }

    private fun startTheApp() {
        device.pressHome()

        device.wait(Until.hasObject(By.pkg(device.launcherPackageName).depth(0)), TIMEOUT)

        val launchIntentForPackage = context.packageManager.getLaunchIntentForPackage(DEBUG_PACKAGE_NAME)
        if (launchIntentForPackage == null) exitFlowWithMessage("$DEBUG_PACKAGE_NAME is not installed")
        val intent = launchIntentForPackage?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        context.startActivity(intent)

        device.wait(
            Until.hasObject(By.pkg(DEBUG_PACKAGE_NAME).depth(0)),
            TIMEOUT
        )
    }

    private fun chooseWpComLogin() {
        val loginWithWpButton = device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "button_login_wpcom")), TIMEOUT)

        if (loginWithWpButton == null) exitFlowWithMessage("You are logged in already")

        loginWithWpButton.click()
    }

    private fun enterEmail() {
        val emailInputField = device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "input")), TIMEOUT)
        val continueButton = device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "login_continue_button")), TIMEOUT)

        emailInputField.text = BuildConfig.QUICK_LOGIN_WP_EMAIL
        continueButton.click()
    }

    private fun enterPassword() {
        val passwordInputField = device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "input")), TIMEOUT)
        val continueButton = device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "bottom_button")), TIMEOUT)

        if (passwordInputField == null) exitFlowWithMessage("Check used email address")

        passwordInputField.text = BuildConfig.QUICK_LOGIN_WP_PASSWORD
        continueButton.click()
    }

    private fun enterSecondFactorIfNeeded() {
        device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "login_otp_button")), TIMEOUT)
            ?: return

        val secondFactorInputField = device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "input")), TIMEOUT)

        val continueButton = device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "bottom_button")), TIMEOUT)

        instrumentation.runOnMainSync {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip() &&
                clipboard.primaryClip!!.getItemAt(0).text.length == SECOND_FACTOR_LENGTH
            ) {
                secondFactorInputField.text = clipboard.primaryClip!!.getItemAt(0).text.toString()
            }
        }
        continueButton.click()

        device.wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "site_list_container")), LONG_TIMEOUT)
    }

    private fun selectSiteIfProvided() {
        if (BuildConfig.QUICK_LOGIN_WP_SITE.isBlank().not()) {
            val siteList = device
                .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "site_list_container")), TIMEOUT)
                ?: return

            val selectedSite = findSelectedSite(siteList) ?: return

            val doneButton = device
                .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "button_primary")), TIMEOUT)

            selectedSite.click()
            doneButton.click()

            device.wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "bottom_nav")), LONG_TIMEOUT)
        }
    }

    private fun findSelectedSite(siteList: UiObject2): UiObject2? {
        fun findSiteToSelect() = device.wait(Until.findObject(By.text(BuildConfig.QUICK_LOGIN_WP_SITE)), SHORT_TIMEOUT)
        var selectedSite = findSiteToSelect()
        var flingsCount = 0
        while (selectedSite == null && flingsCount < SITE_FLINGS_COUNT) {
            siteList.fling(DOWN)
            selectedSite = findSiteToSelect()
            flingsCount++
        }
        return selectedSite
    }

    private fun exitFlowWithMessage(message: String) {
        throw IllegalStateException(message)
    }
}
