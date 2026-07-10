package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooSearchFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given search field with clear action, when rendered, then input ends before clear touch target starts`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                Box(modifier = Modifier.width(SEARCH_FIELD_WIDTH)) {
                    WooSearchField(
                        value = SEARCH_VALUE,
                        onValueChange = {},
                        placeholder = "Search products",
                        onClearClick = {},
                        clearContentDescription = "Clear search",
                    )
                }
            }
        }

        val inputBounds = composeTestRule
            .onNodeWithTag(WooSearchFieldTestTags.INPUT, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val clearButtonBounds = composeTestRule
            .onNodeWithTag(WooSearchFieldTestTags.CLEAR_BUTTON, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot

        assertThat(inputBounds.right).isLessThanOrEqualTo(clearButtonBounds.left)
    }

    @Test
    fun `given resting search field, when placeholder color is resolved, then it matches Figma token`() {
        var placeholderColor: Color? = null
        var expectedColor: Color? = null

        composeTestRule.setContent {
            WooDesignSystemTheme {
                val colors = WooTheme.colors

                placeholderColor = wooSearchFieldPlaceholderColor(colors)
                expectedColor = colors.stateLayer.onSurfaceOpacity16
            }
        }

        composeTestRule.runOnIdle {
            assertThat(placeholderColor).isEqualTo(expectedColor)
        }
    }

    private companion object {
        const val SEARCH_VALUE = "Search products"
        val SEARCH_FIELD_WIDTH = 360.dp
    }
}
