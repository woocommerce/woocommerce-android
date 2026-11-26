package com.woocommerce.android.ui.woopos.settings.details.security

import android.graphics.Bitmap
import android.view.ContextThemeWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WooPosSettingsSecurityScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun phoneInactivityPickerOpensFromItsTitleAtLargeText() =
        checkPicker(DpSize(412.dp, 915.dp), 2f, true, WooPosBreakpoint.Phone)

    @Test
    fun phoneInactivityPickerOpensFromItsTitle() =
        checkPicker(DpSize(412.dp, 915.dp), 1f, true, WooPosBreakpoint.Phone)

    @Test
    fun tabletInactivityPickerOpensFromItsTitle() =
        checkPicker(DpSize(1024.dp, 674.dp), 1f, false, WooPosBreakpoint.SmallTablet)

    private fun checkPicker(size: DpSize, fontScale: Float, dark: Boolean, breakpoint: WooPosBreakpoint) {
        composeTestRule.setContent {
            val configuration = DeviceConfigurationOverride.ForcedSize(size) then
                DeviceConfigurationOverride.WindowSize(size) then
                DeviceConfigurationOverride.WindowInsets(WindowInsetsCompat.CONSUMED) then
                DeviceConfigurationOverride.FontScale(fontScale) then DeviceConfigurationOverride.DarkMode(dark)
            DeviceConfigurationOverride(configuration) {
                val currentContext = LocalContext.current
                val themedContext = remember(currentContext) {
                    ContextThemeWrapper(currentContext, R.style.Theme_WooPos)
                }
                val density = LocalDensity.current
                val hostWindow = LocalWindowInfo.current
                val pixels = with(density) { IntSize(size.width.roundToPx(), size.height.roundToPx()) }
                val window = remember(hostWindow, pixels, size) {
                    object : WindowInfo by hostWindow {
                        override val containerSize = pixels
                        override val containerDpSize = size
                    }
                }
                CompositionLocalProvider(LocalContext provides themedContext, LocalWindowInfo provides window) {
                    assertThat(currentWooPosBreakpoint()).isEqualTo(breakpoint)
                    WooPosTheme {
                        WooPosSettingsSecurityScreen(
                            state = WooPosSettingsSecurityState.Protected(hasCashierPIN = true, removalFailed = false),
                            onSetUpPIN = {},
                            onProtectionTurnedOff = {},
                            onChangePIN = {},
                            onDeleteCashierPIN = {},
                        )
                    }
                }
            }
        }
        saveImage("overview-${size.width.value}-$fontScale-$dark")
        val row = composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_lock_after))
        row.performScrollTo().performClick()
        val never = context.getString(R.string.woopos_security_never)
        composeTestRule.onNodeWithText(never).assertIsDisplayed()
        saveImage("picker-${size.width.value}-$fontScale-$dark")
        composeTestRule.onNodeWithText(never).performClick()
        row.assertIsDisplayed()
        composeTestRule.onNodeWithText(never).assertIsDisplayed()
        saveImage("settings-${size.width.value}-$fontScale-$dark")
    }

    private fun saveImage(name: String) {
        val directory = File(context.getExternalFilesDir(null), "security-review").apply { mkdirs() }
        composeTestRule.waitForIdle()
        val image = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { stream ->
            image.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (!name.startsWith("picker-")) {
            val roots = composeTestRule.onAllNodes(isRoot())
            val content = roots[roots.fetchSemanticsNodes().lastIndex].captureToImage().asAndroidBitmap()
            File(directory, "$name-content.png").outputStream().use { stream ->
                content.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }
}
