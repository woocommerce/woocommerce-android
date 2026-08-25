package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooNoticeBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `given action and dismiss affordances, when tapped, then expose semantics and invoke callbacks`() {
        var actionClicked = false
        var dismissClicked = false
        composeRule.setContent {
            WooDesignSystemTheme {
                WooNoticeBanner(
                    title = "Secure your store with HTTPS",
                    description = "Update the configured address.",
                    actionLabel = "Learn more",
                    onActionClick = { actionClicked = true },
                    dismissContentDescription = "Dismiss HTTPS security notice",
                    onDismissClick = { dismissClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Learn more").performClick()
        composeRule.onNodeWithContentDescription("Dismiss HTTPS security notice").performClick()

        assertThat(actionClicked).isTrue()
        assertThat(dismissClicked).isTrue()
    }

    @Test
    fun `given dark theme and large text, when rendering action banner, then keep all content visible`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                WooDesignSystemTheme(useDarkTheme = true) {
                    WooNoticeBanner(
                        title = "Secure your store with HTTPS",
                        description = "Update the configured address.",
                        actionLabel = "Learn more",
                        onActionClick = {},
                        dismissContentDescription = "Dismiss HTTPS security notice",
                        onDismissClick = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Secure your store with HTTPS").assertIsDisplayed()
        composeRule.onNodeWithText("Learn more").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dismiss HTTPS security notice").assertIsDisplayed()
    }
}
