package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

        OPTIONS.forEach { composeTestRule.onNodeWithText(it).assertIsNotEnabled() }

        // WHEN
        composeTestRule.onNodeWithText(OPTIONS[1]).performClick()

        // THEN
        composeTestRule.runOnIdle { assertThat(callbackCount).isZero() }
    }

    @Test
    fun `given enabled control, when rendered, then every item is focusable and at least 48dp high`() {
        // GIVEN
        givenSegmentControl(selectedIndex = 0)

        // THEN
        OPTIONS.forEach { label ->
            val node = composeTestRule.onNodeWithText(label)
            val semantics = node.fetchSemanticsNode().config

            node.assertIsEnabled().assertHeightIsAtLeast(48.dp)
            assertThat(semantics.getOrNull(SemanticsActions.RequestFocus)).isNotNull()
            node.performSemanticsAction(SemanticsActions.RequestFocus)
            assertThat(node.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused)).isTrue()
        }
    }

    @Test
    fun `given invalid options or selection, when validated, then an argument error is thrown`() {
        // WHEN & THEN
        assertThatThrownBy { validateWooSegmentControl(listOf("Only"), selectedIndex = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { validateWooSegmentControl(listOf("One", " "), selectedIndex = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { validateWooSegmentControl(listOf("One", "Two"), selectedIndex = 2) }
            .isInstanceOf(IllegalArgumentException::class.java)
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
