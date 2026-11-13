package com.woocommerce.android.ui.woopos.lock

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosLockScreenDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val lockTitle: String
        get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.woopos_lock_enter_pin)

    @Test
    fun lockCoversOpenDialogAndConsumesBack() {
        val locked = mutableStateOf(false)
        var protectedDialogDismissed = false
        composeTestRule.setContent {
            WooPosTheme {
                AlertDialog(
                    onDismissRequest = { protectedDialogDismissed = true },
                    confirmButton = {},
                    title = { WooPosText(PROTECTED_TITLE, style = WooPosTypography.Heading) },
                )
                WooPosLockScreenDialog(WooPosLockScreenState(isLocked = locked.value), onUIEvent = {})
            }
        }
        composeTestRule.waitForIdle()

        // WHEN
        composeTestRule.runOnIdle { locked.value = true }
        composeTestRule.waitForIdle()
        device.pressBack()
        device.pressBack()
        composeTestRule.waitForIdle()

        // THEN
        assertThat(device.findObject(By.text(lockTitle))).describedAs("Lock title: %s", lockTitle).isNotNull()
        assertThat(device.findObject(By.text(PROTECTED_TITLE))).isNull()
        assertThat(protectedDialogDismissed).isFalse()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun lockCoversOpenBottomSheetAndConsumesBack() {
        val locked = mutableStateOf(false)
        var protectedSheetDismissed = false
        composeTestRule.setContent {
            WooPosTheme {
                ModalBottomSheet(onDismissRequest = { protectedSheetDismissed = true }) {
                    WooPosText(PROTECTED_TITLE, style = WooPosTypography.Heading)
                }
                WooPosLockScreenDialog(WooPosLockScreenState(isLocked = locked.value), onUIEvent = {})
            }
        }
        composeTestRule.waitForIdle()

        // WHEN
        composeTestRule.runOnIdle { locked.value = true }
        composeTestRule.waitForIdle()
        device.pressBack()
        composeTestRule.waitForIdle()

        // THEN
        assertThat(device.findObject(By.text(lockTitle))).describedAs("Lock title: %s", lockTitle).isNotNull()
        assertThat(device.findObject(By.text(PROTECTED_TITLE))).isNull()
        assertThat(protectedSheetDismissed).isFalse()
    }

    companion object {
        private const val PROTECTED_TITLE = "Protected POS content"
    }
}
