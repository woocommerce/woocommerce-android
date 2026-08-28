package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
import com.woocommerce.android.ui.compose.designsystem.icons.Share
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.within
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooTopAppBarActionsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given blank icon description, when rendered, then illegal argument is thrown`() {
        assertThatIllegalArgumentException().isThrownBy {
            composeTestRule.setContent {
                WooDesignSystemTheme {
                    WooTopAppBar(
                        title = STRING_TITLE,
                        actions = {
                            iconAction(
                                imageVector = WooIcons.Regular.ArrowUpRight,
                                contentDescription = " ",
                                onClick = {},
                            )
                        },
                    )
                }
            }
        }
    }

    @Test
    fun `given blank text action, when rendered, then illegal argument is thrown`() {
        assertThatIllegalArgumentException().isThrownBy {
            composeTestRule.setContent {
                WooDesignSystemTheme {
                    WooTopAppBar(
                        title = STRING_TITLE,
                        actions = {
                            textAction(
                                text = " ",
                                onClick = {},
                            )
                        },
                    )
                }
            }
        }
    }

    @Test
    fun `given string title inline content, when clicked, then content and callback are preserved`() {
        // GIVEN
        var clickCount = 0
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTopAppBar(
                    title = STRING_TITLE,
                    windowInsets = WindowInsets(0),
                    actions = {
                        Text(
                            text = INLINE_ACTION,
                            modifier = Modifier.clickable { clickCount++ },
                        )
                    },
                )
            }
        }

        // WHEN
        composeTestRule.onNodeWithText(INLINE_ACTION).performClick()

        // THEN
        composeTestRule.onNodeWithText(STRING_TITLE).assertExists()
        composeTestRule.runOnIdle { assertThat(clickCount).isOne() }
    }

    @Test
    fun `given composable title inline content, when clicked, then content and callback are preserved`() {
        // GIVEN
        var clickCount = 0
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTopAppBar(
                    title = { Text(COMPOSABLE_TITLE) },
                    windowInsets = WindowInsets(0),
                    actions = {
                        Text(
                            text = INLINE_ACTION,
                            modifier = Modifier.clickable { clickCount++ },
                        )
                    },
                )
            }
        }

        // WHEN
        composeTestRule.onNodeWithText(INLINE_ACTION).performClick()

        // THEN
        composeTestRule.onNodeWithText(COMPOSABLE_TITLE).assertExists()
        composeTestRule.runOnIdle { assertThat(clickCount).isOne() }
    }

    @Test
    fun `given icon actions, when rendered and clicked, then visible 40dp outlines and enabled state are preserved`() {
        // GIVEN
        var enabledClickCount = 0
        var disabledClickCount = 0
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTopAppBar(
                    title = STRING_TITLE,
                    windowInsets = WindowInsets(0),
                    actions = {
                        iconAction(
                            imageVector = WooIcons.Regular.ArrowUpRight,
                            contentDescription = ENABLED_ICON_DESCRIPTION,
                            onClick = { enabledClickCount++ },
                        )
                        iconAction(
                            imageVector = WooIcons.Regular.Share,
                            contentDescription = DISABLED_ICON_DESCRIPTION,
                            onClick = { disabledClickCount++ },
                            enabled = false,
                        )
                    },
                )
            }
        }

        // WHEN
        composeTestRule.onNodeWithContentDescription(ENABLED_ICON_DESCRIPTION).performClick()

        // THEN
        composeTestRule.onNodeWithContentDescription(ENABLED_ICON_DESCRIPTION)
            .assertIsEnabled()
            .assertWidthIsEqualTo(VISIBLE_OUTLINED_ACTION_SIZE)
            .assertHeightIsEqualTo(VISIBLE_OUTLINED_ACTION_SIZE)
        composeTestRule.onNodeWithContentDescription(DISABLED_ICON_DESCRIPTION)
            .assertIsNotEnabled()
            .performClick()
        composeTestRule.runOnIdle {
            assertThat(enabledClickCount).isOne()
            assertThat(disabledClickCount).isZero()
        }
    }

    @Test
    fun `given text actions, when rendered and clicked, then text and enabled state are preserved`() {
        // GIVEN
        var enabledClickCount = 0
        var disabledClickCount = 0
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTopAppBar(
                    title = STRING_TITLE,
                    windowInsets = WindowInsets(0),
                    actions = {
                        textAction(
                            text = ENABLED_TEXT_ACTION,
                            onClick = { enabledClickCount++ },
                        )
                        textAction(
                            text = DISABLED_TEXT_ACTION,
                            onClick = { disabledClickCount++ },
                            enabled = false,
                        )
                    },
                )
            }
        }

        // WHEN
        composeTestRule.onNodeWithText(ENABLED_TEXT_ACTION).performClick()

        // THEN
        composeTestRule.onNodeWithText(ENABLED_TEXT_ACTION).assertIsEnabled()
        composeTestRule.onNodeWithText(DISABLED_TEXT_ACTION)
            .assertIsNotEnabled()
            .performClick()
        composeTestRule.runOnIdle {
            assertThat(enabledClickCount).isOne()
            assertThat(disabledClickCount).isZero()
        }
    }

    @Test
    fun `given empty conditional between icons, when LTR, then 48dp interactive bounds have one space1 gap`() {
        // GIVEN
        var expectedGapPx = 0
        var expectedInteractiveSizePx = 0
        var firstBounds: Rect? = null
        var secondBounds: Rect? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                WooDesignSystemTheme {
                    expectedGapPx = with(LocalDensity.current) { WooTheme.spacing.space1.roundToPx() }
                    expectedInteractiveSizePx = with(LocalDensity.current) { INTERACTIVE_ACTION_SIZE.roundToPx() }
                    WooTopAppBar(
                        title = STRING_TITLE,
                        windowInsets = WindowInsets(0),
                        actions = {
                            iconAction(
                                imageVector = WooIcons.Regular.ArrowUpRight,
                                contentDescription = FIRST_ICON_DESCRIPTION,
                                onClick = {},
                                modifier = Modifier.onGloballyPositioned { firstBounds = it.boundsInRoot() },
                            )
                            if (false) {
                                Box(modifier = Modifier.size(PHANTOM_ACTION_SIZE))
                            }
                            iconAction(
                                imageVector = WooIcons.Regular.Share,
                                contentDescription = SECOND_ICON_DESCRIPTION,
                                onClick = {},
                                modifier = Modifier.onGloballyPositioned { secondBounds = it.boundsInRoot() },
                            )
                        },
                    )
                }
            }
        }

        // THEN
        composeTestRule.runOnIdle {
            val firstActionBounds = requireNotNull(firstBounds)
            val secondActionBounds = requireNotNull(secondBounds)
            assertInteractiveLayoutSize(firstActionBounds, expectedInteractiveSizePx)
            assertInteractiveLayoutSize(secondActionBounds, expectedInteractiveSizePx)
            assertThat(secondActionBounds.left - firstActionBounds.right)
                .isCloseTo(expectedGapPx.toFloat(), within(LAYOUT_BOUND_TOLERANCE_PX))
        }
    }

    @Test
    fun `given custom before icon, when LTR, then icon has 48dp interactive bounds and ordered space1 gap`() {
        // GIVEN
        var expectedGapPx = 0
        var expectedInteractiveSizePx = 0
        var customBounds: Rect? = null
        var iconBounds: Rect? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                WooDesignSystemTheme {
                    expectedGapPx = with(LocalDensity.current) { WooTheme.spacing.space1.roundToPx() }
                    expectedInteractiveSizePx = with(LocalDensity.current) { INTERACTIVE_ACTION_SIZE.roundToPx() }
                    WooTopAppBar(
                        title = STRING_TITLE,
                        windowInsets = WindowInsets(0),
                        actions = {
                            Box(
                                modifier = Modifier
                                    .size(INLINE_CUSTOM_SIZE)
                                    .onGloballyPositioned { customBounds = it.boundsInRoot() },
                            )
                            iconAction(
                                imageVector = WooIcons.Regular.ArrowUpRight,
                                contentDescription = ENABLED_ICON_DESCRIPTION,
                                onClick = {},
                                modifier = Modifier.onGloballyPositioned { iconBounds = it.boundsInRoot() },
                            )
                        },
                    )
                }
            }
        }

        // THEN
        composeTestRule.runOnIdle {
            val inlineBounds = requireNotNull(customBounds)
            val iconActionBounds = requireNotNull(iconBounds)
            assertInteractiveLayoutSize(iconActionBounds, expectedInteractiveSizePx)
            assertThat(inlineBounds.left).isLessThan(iconActionBounds.left)
            assertThat(iconActionBounds.left - inlineBounds.right)
                .isCloseTo(expectedGapPx.toFloat(), within(LAYOUT_BOUND_TOLERANCE_PX))
        }
    }

    @Test
    fun `given two icons, when RTL, then 48dp interactive bounds have symmetric space1 gap`() {
        // GIVEN
        var expectedGapPx = 0
        var expectedInteractiveSizePx = 0
        var firstBounds: Rect? = null
        var secondBounds: Rect? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                WooDesignSystemTheme {
                    expectedGapPx = with(LocalDensity.current) { WooTheme.spacing.space1.roundToPx() }
                    expectedInteractiveSizePx = with(LocalDensity.current) { INTERACTIVE_ACTION_SIZE.roundToPx() }
                    WooTopAppBar(
                        title = STRING_TITLE,
                        windowInsets = WindowInsets(0),
                        actions = {
                            iconAction(
                                imageVector = WooIcons.Regular.ArrowUpRight,
                                contentDescription = FIRST_ICON_DESCRIPTION,
                                onClick = {},
                                modifier = Modifier.onGloballyPositioned { firstBounds = it.boundsInRoot() },
                            )
                            iconAction(
                                imageVector = WooIcons.Regular.Share,
                                contentDescription = SECOND_ICON_DESCRIPTION,
                                onClick = {},
                                modifier = Modifier.onGloballyPositioned { secondBounds = it.boundsInRoot() },
                            )
                        },
                    )
                }
            }
        }

        // THEN
        composeTestRule.runOnIdle {
            val firstActionBounds = requireNotNull(firstBounds)
            val secondActionBounds = requireNotNull(secondBounds)
            assertInteractiveLayoutSize(firstActionBounds, expectedInteractiveSizePx)
            assertInteractiveLayoutSize(secondActionBounds, expectedInteractiveSizePx)
            assertThat(firstActionBounds.left).isGreaterThan(secondActionBounds.left)
            assertThat(firstActionBounds.left - secondActionBounds.right)
                .isCloseTo(expectedGapPx.toFloat(), within(LAYOUT_BOUND_TOLERANCE_PX))
        }
    }

    @Test
    fun `given overflow action, when trigger is clicked, then outlined trigger opens a dismissible menu`() {
        // GIVEN
        var selected: String? = null
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTopAppBar(
                    title = STRING_TITLE,
                    windowInsets = WindowInsets(0),
                    actions = {
                        overflowAction(contentDescription = OVERFLOW_DESCRIPTION) { dismiss ->
                            WooOverflowMenuItem(
                                text = OVERFLOW_ITEM,
                                onClick = {
                                    dismiss()
                                    selected = OVERFLOW_ITEM
                                },
                            )
                        }
                    },
                )
            }
        }
        composeTestRule.onNodeWithContentDescription(OVERFLOW_DESCRIPTION)
            .assertWidthIsEqualTo(VISIBLE_OUTLINED_ACTION_SIZE)
            .assertHeightIsEqualTo(VISIBLE_OUTLINED_ACTION_SIZE)
        composeTestRule.onNodeWithText(OVERFLOW_ITEM).assertDoesNotExist()

        // WHEN
        composeTestRule.onNodeWithContentDescription(OVERFLOW_DESCRIPTION).performClick()
        composeTestRule.onNodeWithText(OVERFLOW_ITEM).performClick()

        // THEN
        composeTestRule.runOnIdle { assertThat(selected).isEqualTo(OVERFLOW_ITEM) }
        composeTestRule.onNodeWithText(OVERFLOW_ITEM).assertDoesNotExist()
    }

    private fun assertInteractiveLayoutSize(
        bounds: Rect,
        expectedSizePx: Int,
    ) {
        assertThat(bounds.width)
            .isCloseTo(expectedSizePx.toFloat(), within(LAYOUT_BOUND_TOLERANCE_PX))
        assertThat(bounds.height)
            .isCloseTo(expectedSizePx.toFloat(), within(LAYOUT_BOUND_TOLERANCE_PX))
    }

    private companion object {
        const val STRING_TITLE = "String title"
        const val COMPOSABLE_TITLE = "Composable title"
        const val INLINE_ACTION = "Inline action"
        const val ENABLED_ICON_DESCRIPTION = "Open"
        const val DISABLED_ICON_DESCRIPTION = "Share disabled"
        const val FIRST_ICON_DESCRIPTION = "First"
        const val SECOND_ICON_DESCRIPTION = "Second"
        const val ENABLED_TEXT_ACTION = "Save"
        const val DISABLED_TEXT_ACTION = "Done"
        const val OVERFLOW_DESCRIPTION = "More options"
        const val OVERFLOW_ITEM = "Duplicate"
        const val LAYOUT_BOUND_TOLERANCE_PX = 0.01f
        val VISIBLE_OUTLINED_ACTION_SIZE = 40.dp
        val INTERACTIVE_ACTION_SIZE = 48.dp
        val INLINE_CUSTOM_SIZE = 12.dp
        val PHANTOM_ACTION_SIZE = 20.dp
    }
}
