package com.woocommerce.android.quicklogin

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction.DOWN
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

private const val SHORT_TIMEOUT = 2000L
private const val TIMEOUT = 5000L
private const val LONG_TIMEOUT = 60000L

private const val SECOND_FACTOR_LENGTH = 6
private const val SITE_FLINGS_COUNT = 10

class QuickLoginHelper(private val packageName: String) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    fun loginWithWordpress(
        email: String,
        password: String,
        webSite: String,
    ) {
        startTheApp()
        chooseWpComLogin()
        enterEmail(email)
        enterPassword(password)
        enterSecondFactorIfNeeded()
        selectSiteIfProvided(webSite)
    }

    private fun startTheApp() {
        device.pressHome()

        device.wait(Until.hasObject(By.pkg(device.launcherPackageName).depth(0)), TIMEOUT)

        val launchIntentForPackage = context.packageManager.getLaunchIntentForPackage(packageName)
        val intent = launchIntentForPackage?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        context.startActivity(intent)

        device.wait(
            Until.hasObject(By.pkg(packageName).depth(0)),
            TIMEOUT
        )
    }

    private fun chooseWpComLogin() {
        val loginWithWpButton = device
            .wait(Until.findObject(By.res(packageName, "button_login_wpcom")), TIMEOUT) ?: return

        loginWithWpButton.click()
    }

    private fun enterEmail(email: String) {
        val emailInputField = device
            .wait(Until.findObject(By.res(packageName, "input")), TIMEOUT)
        val continueButton = device
            .wait(Until.findObject(By.res(packageName, "login_continue_button")), TIMEOUT)

        emailInputField.text = email
        continueButton.click()
    }

    private fun enterPassword(password: String) {
        val passwordInputField = device
            .wait(Until.findObject(By.res(packageName, "input")), TIMEOUT)
        val continueButton = device
            .wait(Until.findObject(By.res(packageName, "bottom_button")), TIMEOUT)

        if (passwordInputField == null) exitFlowWithMessage("Check used email address")

        passwordInputField.text = password
        continueButton.click()
    }

    private fun enterSecondFactorIfNeeded() {
        device
            .wait(Until.findObject(By.res(packageName, "login_otp_button")), TIMEOUT)
            ?: return

        val secondFactorInputField = device
            .wait(Until.findObject(By.res(packageName, "input")), TIMEOUT)

        val continueButton = device
            .wait(Until.findObject(By.res(packageName, "bottom_button")), TIMEOUT)

        instrumentation.runOnMainSync {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip() &&
                clipboard.primaryClip!!.getItemAt(0).text.length == SECOND_FACTOR_LENGTH
            ) {
                secondFactorInputField.text = clipboard.primaryClip!!.getItemAt(0).text.toString()
            }
        }
        continueButton.click()

        device.wait(Until.findObject(By.res(packageName, "site_list_container")), LONG_TIMEOUT)
    }

    private fun selectSiteIfProvided(webSite: String) {
        if (webSite.isBlank().not()) {
            val siteList = device
                .wait(Until.findObject(By.res(packageName, "site_list_container")), TIMEOUT)
                ?: return

            val selectedSite = findSelectedSite(siteList, webSite) ?: return

            val doneButton = device
                .wait(Until.findObject(By.res(packageName, "button_primary")), TIMEOUT)

            selectedSite.click()
            doneButton.click()

            device.wait(Until.findObject(By.res(packageName, "bottom_nav")), LONG_TIMEOUT)
        }
    }

    private fun findSelectedSite(siteList: UiObject2, website: String): UiObject2? {
        fun findSiteToSelect() = device.wait(Until.findObject(By.text(website)), SHORT_TIMEOUT)
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
