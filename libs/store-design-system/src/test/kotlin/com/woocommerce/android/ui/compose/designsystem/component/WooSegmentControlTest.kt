package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooSegmentControlTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given segment control, when rendered, then group and radio selection semantics are exposed`() {
        // GIVEN
        givenSegmentControl(selectedIndex = 1)

        // WHEN
        val groupSemantics = composeTestRule.onNodeWithTag(GROUP_TAG).fetchSemanticsNode().config
        val selectedSemantics = composeTestRule.onNodeWithText(OPTIONS[1]).fetchSemanticsNode().config

        // THEN
        assertThat(groupSemantics.getOrNull(SemanticsProperties.SelectableGroup)).isNotNull()
        assertThat(selectedSemantics.getOrNull(SemanticsProperties.Role)).isEqualTo(Role.RadioButton)
        composeTestRule.onNodeWithText(OPTIONS[0]).assertIsNotSelected()
        composeTestRule.onNodeWithText(OPTIONS[1]).assertIsSelected()
    }

    @Test
    fun `given controlled selection, when another segment is clicked, then callback receives index only`() {
        // GIVEN
        var callbackIndex: Int? = null
        givenSegmentControl(selectedIndex = 0, onSelectedIndexChange = { callbackIndex = it })

        // WHEN
        composeTestRule.onNodeWithText(OPTIONS[2]).performClick()

        // THEN
        composeTestRule.runOnIdle { assertThat(callbackIndex).isEqualTo(2) }
        composeTestRule.onNodeWithText(OPTIONS[0]).assertIsSelected()
        composeTestRule.onNodeWithText(OPTIONS[2]).assertIsNotSelected()
    }

    @Test
    fun `given disabled control, when a segment is clicked, then items are disabled and callback is suppressed`() {
        // GIVEN
        var callbackCount = 0
        givenSegmentControl(
            selectedIndex = 0,
            enabled = false,
            onSelectedIndexChange = { callbackCount++ },
        )

        // WHEN
        val disabledItem = composeTestRule.onNodeWithText(OPTIONS[1]).assertIsNotEnabled()
        disabledItem.performClick()

        // THEN
        composeTestRule.runOnIdle { assertThat(callbackCount).isZero() }
    }

    @Test
    fun `given enabled control, when rendered, then an item has a 48dp interaction target`() {
        // GIVEN
        givenSegmentControl(selectedIndex = 0)

        // THEN
        composeTestRule.onNodeWithText(OPTIONS[1]).assertHeightIsAtLeast(48.dp)
    }

    private fun givenSegmentControl(
        selectedIndex: Int,
        enabled: Boolean = true,
        onSelectedIndexChange: (Int) -> Unit = {},
    ) {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooSegmentControl(
                    options = OPTIONS,
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = onSelectedIndexChange,
                    enabled = enabled,
                    modifier = Modifier
                        .width(300.dp)
                        .testTag(GROUP_TAG),
                )
            }
        }
    }

    private companion object {
        val OPTIONS = listOf("Net sales", "Orders", "Visitors")
        const val GROUP_TAG = "segment-control"
    }
}
