package com.woocommerce.android.quickloginhelper

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.woocommerce.android.AppPrefs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalStateException

private const val DEBUG_PACKAGE_NAME = "com.woocommerce.android.prealpha"
private const val TIMEOUT = 5000L

@RunWith(AndroidJUnit4::class)
class QuickLoginHelperWordpress {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun init() {
        AppPrefs.init(context)
    }

    @Test
    fun loginWithWordpress() {
        startTheApp()
        chooseWpComLogin()
        enterEmail()
        enterPassword()
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

        emailInputField.text = ""
        continueButton.click()
    }

    private fun enterPassword() {
        val passwordInputField = device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "input")), TIMEOUT)
        val continueButton = device
            .wait(Until.findObject(By.res(DEBUG_PACKAGE_NAME, "bottom_button")), TIMEOUT)

        if (passwordInputField == null) exitFlowWithMessage("Check used email address")

        passwordInputField.text = ""
        continueButton.click()
    }

    private fun exitFlowWithMessage(message: String) {
        throw IllegalStateException(message)
    }
}
