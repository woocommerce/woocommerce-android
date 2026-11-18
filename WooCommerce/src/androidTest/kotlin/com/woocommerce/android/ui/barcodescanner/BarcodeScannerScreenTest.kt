package com.woocommerce.android.ui.barcodescanner

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.LegacyWooThemeWithBackground
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BarcodeScannerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var hardwareScannerInfoOpened = false

    @Test
    fun givenCameraPermissionDeniedWhenExternalScannerIsSupportedThenScannerSetupIsAvailable() {
        // WHEN
        givenScreen(onHardwareScannerInfoClicked = { hardwareScannerInfoOpened = true })

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.barcode_scanning_alert_dialog_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.barcode_scanning_hardware_scanner_info_link))
            .assertIsDisplayed()
            .performClick()
        composeTestRule.runOnIdle {
            assertThat(hardwareScannerInfoOpened).isTrue()
        }
    }

    @Test
    fun givenCameraPermissionDeniedWhenExternalScannerIsNotSupportedThenPermissionDialogIsShown() {
        // WHEN
        givenScreen(onHardwareScannerInfoClicked = null)

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.barcode_scanning_alert_dialog_dismiss_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.barcode_scanning_hardware_scanner_info_link))
            .assertDoesNotExist()
    }

    private fun givenScreen(onHardwareScannerInfoClicked: (() -> Unit)?) {
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = DeniedActivityResultRegistry()
        }
        val permissionState = BarcodeScanningViewModel.PermissionState.PermanentlyDenied(
            title = R.string.barcode_scanning_alert_dialog_title,
            message = R.string.barcode_scanning_alert_dialog_permanently_denied_message,
            ctaLabel = R.string.barcode_scanning_alert_dialog_permanently_denied_cta_label,
            dismissCtaLabel = R.string.barcode_scanning_alert_dialog_dismiss_label,
            ctaAction = {},
            dismissCtaAction = {},
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                LegacyWooThemeWithBackground {
                    BarcodeScannerScreen(
                        onNewFrame = {},
                        onBindingException = {},
                        permissionState = permissionState,
                        onResult = {},
                        overlayContent = {},
                        onHardwareScannerInfoClicked = onHardwareScannerInfoClicked,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private class DeniedActivityResultRegistry : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            dispatchResult(requestCode, false)
        }
    }
}
