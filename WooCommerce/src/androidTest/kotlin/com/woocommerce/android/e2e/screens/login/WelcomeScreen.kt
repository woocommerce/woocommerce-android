package com.woocommerce.android.e2e.screens.login

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.TabNavComponent

class WelcomeScreen : Screen {
    companion object {
        private const val QR_PROLOGUE_TIMEOUT = 5000L

        fun logoutIfNeeded(composeTestRule: ComposeContentTestRule): WelcomeScreen {
            if (isElementDisplayed(R.id.dashboard)) {
                TabNavComponent()
                    .gotoMoreMenuScreen()
                    .openSettings(composeTestRule)
                    .logOut()
            }

            Thread.sleep(1000)

            return skipCarouselIfNeeded()
        }

        fun skipCarouselIfNeeded(): WelcomeScreen {
            return if (Screen.isElementDisplayed(CarouselScreen.SKIP_BUTTON)) {
                CarouselScreen().skip()
            } else {
                WelcomeScreen()
            }
        }
    }

    constructor() : super(R.id.button_login_store)

    fun selectLogin(): SiteAddressScreen {
        clickOn(R.id.button_login_store)
        proceedThroughQrLoginPrologueIfNeeded()
        return SiteAddressScreen()
    }

    // With QR login enabled, the primary button opens the QR login prologue; continue to the
    // site-address screen through its fallback link. UiAutomator is used because the QR prologue
    // is Compose and there is no ComposeTestRule available here.
    private fun proceedThroughQrLoginPrologueIfNeeded() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val fallbackLinkText = instrumentation.targetContext
            .getString(R.string.login_qr_prologue_fallback_link)
        UiDevice.getInstance(instrumentation)
            .wait(Until.findObject(By.text(fallbackLinkText)), QR_PROLOGUE_TIMEOUT)
            ?.click()
    }

    fun selectWPCOMLogin(): EmailAddressScreen {
        clickOn(R.id.button_login_wpcom)
        return EmailAddressScreen()
    }
}
