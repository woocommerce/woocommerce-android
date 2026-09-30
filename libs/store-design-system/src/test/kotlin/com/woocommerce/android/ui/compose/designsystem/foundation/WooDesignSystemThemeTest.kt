package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooDesignSystemThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given theme with row weight, when rendered, then the modifier reaches the parent row`() {
        // GIVEN
        composeTestRule.setContent {
            Row(modifier = Modifier.width(200.dp)) {
                WooDesignSystemTheme(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(THEME_TAG),
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp),
                )
            }
        }

        // THEN
        composeTestRule.onNodeWithTag(THEME_TAG).assertWidthIsEqualTo(100.dp)
    }

    @Test
    fun `given theme without a modifier, when rendered, then it wraps its content`() {
        // GIVEN
        composeTestRule.setContent {
            Row(modifier = Modifier.width(200.dp)) {
                WooDesignSystemTheme {
                    Spacer(
                        modifier = Modifier
                            .testTag(CONTENT_TAG)
                            .width(20.dp)
                            .height(10.dp),
                    )
                }
            }
        }

        // THEN
        composeTestRule.onNodeWithTag(CONTENT_TAG).assertWidthIsEqualTo(20.dp)
    }

    private companion object {
        const val THEME_TAG = "design-system-theme"
        const val CONTENT_TAG = "design-system-theme-content"
    }
}
