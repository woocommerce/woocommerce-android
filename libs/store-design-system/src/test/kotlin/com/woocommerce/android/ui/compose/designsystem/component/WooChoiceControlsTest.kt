package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooColors
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooStroke
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooChoiceControlsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given constrained filter chip with two icons, when rendered, then label remains single line`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                Box(modifier = Modifier.requiredWidth(CONSTRAINED_CHIP_WIDTH)) {
                    WooFilterChip(
                        selected = true,
                        onClick = {},
                        label = CHIP_LABEL,
                        leadingIcon = { Box(modifier = Modifier.size(CHIP_ICON_SIZE)) },
                        trailingIcon = { Box(modifier = Modifier.size(CHIP_ICON_SIZE)) },
                    )
                }
            }
        }

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        val layoutResultAction = composeTestRule
            .onNodeWithText(CHIP_LABEL, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action

        composeTestRule.runOnIdle {
            assertThat(layoutResultAction?.invoke(textLayoutResults)).isTrue()
            assertThat(textLayoutResults.single().lineCount).isEqualTo(1)
        }
    }

    @Test
    fun `given choice controls demo, when enabled controls are clicked, then selected state updates`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooChoiceControlsDemo()
            }
        }

        composeTestRule.onNodeWithTag(WooChoiceControlsDemoTags.CHECKED_CHECKBOX)
            .assertIsOn()
            .performClick()
            .assertIsOff()
        composeTestRule.onNodeWithTag(WooChoiceControlsDemoTags.UNSELECTED_RADIO)
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
        composeTestRule.onAllNodesWithText(FILTER_CHIP_LABEL)
            .onFirst()
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun `given filter chip, when rendered, then full minimum touch target is clickable`() {
        composeTestRule.setContent {
            var selected by mutableStateOf(false)

            WooDesignSystemTheme {
                WooFilterChip(
                    selected = selected,
                    onClick = { selected = !selected },
                    label = FILTER_CHIP_LABEL,
                    modifier = Modifier.testTag(FILTER_CHIP_TAG),
                )
            }
        }

        val filterChip = composeTestRule.onNodeWithTag(FILTER_CHIP_TAG)
        val filterChipBounds = filterChip.fetchSemanticsNode().boundsInRoot

        filterChip.assertIsOff()
        composeTestRule.onRoot()
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET_SIZE)
            .performTouchInput {
                click(Offset(filterChipBounds.center.x, filterChipBounds.top - 1f))
            }
        filterChip.assertIsOn()
    }

    @Test
    fun `given action chip, when rendered, then it has button semantics and a minimum touch target`() {
        var clicked = false
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooActionChip(
                    onClick = { clicked = true },
                    label = ACTION_CHIP_LABEL,
                    modifier = Modifier.testTag(ACTION_CHIP_TAG),
                )
            }
        }

        val actionChip = composeTestRule.onNodeWithTag(ACTION_CHIP_TAG)
        val actionChipBounds = actionChip.fetchSemanticsNode().boundsInRoot
        val semantics = actionChip.fetchSemanticsNode().config

        assertThat(semantics.getOrNull(SemanticsProperties.Role)).isEqualTo(Role.Button)
        assertThat(semantics.getOrNull(SemanticsProperties.ToggleableState)).isNull()
        composeTestRule.onRoot()
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET_SIZE)
            .performTouchInput {
                click(Offset(actionChipBounds.center.x, actionChipBounds.top - 1f))
            }
        composeTestRule.runOnIdle { assertThat(clicked).isTrue() }
    }

    @Test
    fun `given indeterminate checkbox, when rendered and clicked, then tri-state semantics and click work`() {
        var clicked = false

        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooCheckbox(
                    state = ToggleableState.Indeterminate,
                    onClick = { clicked = true },
                    modifier = Modifier.testTag(TRISTATE_CHECKBOX_TAG),
                )
            }
        }

        val toggleableState = composeTestRule.onNodeWithTag(TRISTATE_CHECKBOX_TAG)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ToggleableState)

        assertThat(toggleableState).isEqualTo(ToggleableState.Indeterminate)

        composeTestRule.onNodeWithTag(TRISTATE_CHECKBOX_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertThat(clicked).isTrue()
        }
    }

    @Test
    fun `given indeterminate checkbox style, when resolved, then it matches Figma tokens`() {
        lateinit var style: WooCheckboxStyle
        lateinit var colors: WooColors
        lateinit var stroke: WooStroke

        composeTestRule.setContent {
            WooDesignSystemTheme {
                colors = WooTheme.colors
                stroke = WooTheme.stroke
                style = wooCheckboxStyle(
                    state = ToggleableState.Indeterminate,
                    enabled = true,
                    isError = false,
                    colors = colors,
                    stroke = stroke,
                )
            }
        }

        composeTestRule.runOnIdle {
            assertThat(style.containerColor).isEqualTo(colors.primary)
            assertThat(style.borderColor).isEqualTo(Color.Transparent)
            assertThat(style.markColor).isEqualTo(colors.onPrimary)
            assertThat(style.borderWidth).isEqualTo(stroke.none)
            assertThat(style.mark).isEqualTo(WooCheckboxMark.Indeterminate)
        }
    }

    @Test
    fun `given error unselected checkbox style, when resolved, then it matches Figma tokens`() {
        lateinit var style: WooCheckboxStyle
        lateinit var colors: WooColors
        lateinit var stroke: WooStroke

        composeTestRule.setContent {
            WooDesignSystemTheme {
                colors = WooTheme.colors
                stroke = WooTheme.stroke
                style = wooCheckboxStyle(
                    state = ToggleableState.Off,
                    enabled = true,
                    isError = true,
                    colors = colors,
                    stroke = stroke,
                )
            }
        }

        composeTestRule.runOnIdle {
            assertThat(style.containerColor).isEqualTo(Color.Transparent)
            assertThat(style.borderColor).isEqualTo(colors.error)
            assertThat(style.markColor).isEqualTo(Color.Transparent)
            assertThat(style.borderWidth).isEqualTo(stroke.medium)
            assertThat(style.mark).isEqualTo(WooCheckboxMark.None)
        }
    }

    @Test
    fun `given disabled error indeterminate checkbox style, when resolved, then disabled state wins`() {
        lateinit var style: WooCheckboxStyle
        lateinit var colors: WooColors
        lateinit var stroke: WooStroke

        composeTestRule.setContent {
            WooDesignSystemTheme {
                colors = WooTheme.colors
                stroke = WooTheme.stroke
                style = wooCheckboxStyle(
                    state = ToggleableState.Indeterminate,
                    enabled = false,
                    isError = true,
                    colors = colors,
                    stroke = stroke,
                )
            }
        }

        composeTestRule.runOnIdle {
            assertThat(style.containerColor).isEqualTo(colors.stateLayers.onSurface.opacity16)
            assertThat(style.borderColor).isEqualTo(Color.Transparent)
            assertThat(style.markColor).isEqualTo(colors.onPrimary)
            assertThat(style.borderWidth).isEqualTo(stroke.none)
            assertThat(style.mark).isEqualTo(WooCheckboxMark.Indeterminate)
        }
    }

    @Test
    fun `given selected radio style, when resolved, then it matches Figma tokens`() {
        lateinit var style: WooRadioButtonStyle
        lateinit var colors: WooColors
        lateinit var stroke: WooStroke

        composeTestRule.setContent {
            WooDesignSystemTheme {
                colors = WooTheme.colors
                stroke = WooTheme.stroke
                style = wooRadioButtonStyle(
                    selected = true,
                    enabled = true,
                    colors = colors,
                    stroke = stroke,
                )
            }
        }

        composeTestRule.runOnIdle {
            assertThat(style.containerColor).isEqualTo(colors.primary)
            assertThat(style.borderColor).isEqualTo(Color.Transparent)
            assertThat(style.dotColor).isEqualTo(colors.onPrimary)
            assertThat(style.borderWidth).isEqualTo(stroke.none)
        }
    }

    @Test
    fun `given disabled unselected radio style, when resolved, then it matches Figma tokens`() {
        lateinit var style: WooRadioButtonStyle
        lateinit var colors: WooColors
        lateinit var stroke: WooStroke

        composeTestRule.setContent {
            WooDesignSystemTheme {
                colors = WooTheme.colors
                stroke = WooTheme.stroke
                style = wooRadioButtonStyle(
                    selected = false,
                    enabled = false,
                    colors = colors,
                    stroke = stroke,
                )
            }
        }

        composeTestRule.runOnIdle {
            assertThat(style.containerColor).isEqualTo(Color.Transparent)
            assertThat(style.borderColor).isEqualTo(colors.stateLayers.onSurface.opacity16)
            assertThat(style.dotColor).isEqualTo(Color.Transparent)
            assertThat(style.borderWidth).isEqualTo(stroke.medium)
        }
    }

    @Test
    fun `given selected filter chip style, when resolved, then it matches Figma tokens`() {
        lateinit var style: WooFilterChipStyle
        lateinit var colors: WooColors
        lateinit var stroke: WooStroke

        composeTestRule.setContent {
            WooDesignSystemTheme {
                colors = WooTheme.colors
                stroke = WooTheme.stroke
                style = wooFilterChipStyle(
                    selected = true,
                    enabled = true,
                    colors = colors,
                    stroke = stroke,
                )
            }
        }

        composeTestRule.runOnIdle {
            assertThat(style.containerColor).isEqualTo(colors.container.secondaryContainer)
            assertThat(style.contentColor).isEqualTo(colors.surface.onDefault)
            assertThat(style.borderColor).isEqualTo(Color.Transparent)
            assertThat(style.borderWidth).isEqualTo(stroke.none)
        }
    }

    @Test
    fun `given unselected filter chip style, when resolved, then it matches Figma tokens`() {
        lateinit var style: WooFilterChipStyle
        lateinit var colors: WooColors
        lateinit var stroke: WooStroke

        composeTestRule.setContent {
            WooDesignSystemTheme {
                colors = WooTheme.colors
                stroke = WooTheme.stroke
                style = wooFilterChipStyle(
                    selected = false,
                    enabled = true,
                    colors = colors,
                    stroke = stroke,
                )
            }
        }

        composeTestRule.runOnIdle {
            assertThat(style.containerColor).isEqualTo(colors.surface.default)
            assertThat(style.contentColor).isEqualTo(colors.surface.onDefault)
            assertThat(style.borderColor).isEqualTo(colors.outlineVariant)
            assertThat(style.borderWidth).isEqualTo(stroke.regular)
        }
    }

    private companion object {
        const val CHIP_LABEL = "Selected"
        const val ACTION_CHIP_LABEL = "Sort by"
        const val ACTION_CHIP_TAG = "WooActionChip"
        const val FILTER_CHIP_LABEL = "Filter"
        const val FILTER_CHIP_TAG = "WooFilterChipMinTouchTarget"
        const val TRISTATE_CHECKBOX_TAG = "WooTriStateCheckbox"
        val CHIP_ICON_SIZE = 14.dp
        val CONSTRAINED_CHIP_WIDTH = 96.dp
        val MIN_TOUCH_TARGET_SIZE = 48.dp
    }
}
