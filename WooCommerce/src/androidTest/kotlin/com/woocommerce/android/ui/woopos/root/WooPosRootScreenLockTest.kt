package com.woocommerce.android.ui.woopos.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosRootScreenLockTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun activityHidesProtectedPixelsWhileLockedAndKeepsItsContent() {
        val locked = mutableStateOf(false)
        var contentCreations = 0
        var background = Color.Unspecified
        composeTestRule.setContent {
            WooPosTheme {
                background = MaterialTheme.colorScheme.surface
                WooPosRootScreen(
                    state = WooPosLockScreenState(isLocked = locked.value),
                    onUIEvent = {},
                    modifier = Modifier.testTag(ROOT),
                ) {
                    remember { contentCreations++ }
                    Box(Modifier.fillMaxSize().background(Color.Red))
                }
            }
        }
        assertThat(activityPixel()).isEqualTo(Color.Red)

        // WHEN
        composeTestRule.runOnIdle { locked.value = true }

        // THEN the Activity window itself is opaque, independently of the dialog window.
        assertThat(activityPixel()).isEqualTo(background)
        assertThat(contentCreations).isEqualTo(1)

        // WHEN
        composeTestRule.runOnIdle { locked.value = false }

        // THEN
        assertThat(activityPixel()).isEqualTo(Color.Red)
        assertThat(contentCreations).isEqualTo(1)
    }

    private fun activityPixel(): Color {
        val pixels = composeTestRule.onNodeWithTag(ROOT).captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    private companion object {
        const val ROOT = "POS activity content"
    }
}
