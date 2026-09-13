package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
import com.woocommerce.android.ui.compose.designsystem.icons.Share
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
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
                            IconAction(
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
                            TextAction(
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
    fun `given blank overflow description, when rendered, then illegal argument is thrown`() {
        assertThatIllegalArgumentException().isThrownBy {
            composeTestRule.setContent {
                WooDesignSystemTheme {
                    WooTopAppBar(
                        title = STRING_TITLE,
                        actions = {
                            OverflowAction(contentDescription = " ") {
                                WooOverflowMenuItem(text = OVERFLOW_ITEM, onClick = {})
                            }
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
    fun `given icon actions, when rendered and clicked, then enabled state and callbacks are preserved`() {
        // GIVEN
        var enabledClickCount = 0
        var disabledClickCount = 0
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTopAppBar(
                    title = STRING_TITLE,
                    windowInsets = WindowInsets(0),
                    actions = {
                        IconAction(
                            imageVector = WooIcons.Regular.ArrowUpRight,
                            contentDescription = ENABLED_ICON_DESCRIPTION,
                            onClick = { enabledClickCount++ },
                        )
                        IconAction(
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
        composeTestRule.onNodeWithContentDescription(ENABLED_ICON_DESCRIPTION).assertIsEnabled()
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
                        TextAction(
                            text = ENABLED_TEXT_ACTION,
                            onClick = { enabledClickCount++ },
                        )
                        TextAction(
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
    fun `given overflow action, when trigger is clicked, then menu opens and dismisses on selection`() {
        // GIVEN
        var selected: String? = null
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTopAppBar(
                    title = STRING_TITLE,
                    windowInsets = WindowInsets(0),
                    actions = {
                        OverflowAction(contentDescription = OVERFLOW_DESCRIPTION) { dismiss ->
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
        composeTestRule.onNodeWithText(OVERFLOW_ITEM).assertDoesNotExist()

        // WHEN
        composeTestRule.onNodeWithContentDescription(OVERFLOW_DESCRIPTION).performClick()
        composeTestRule.onNodeWithText(OVERFLOW_ITEM).performClick()

        // THEN
        composeTestRule.runOnIdle { assertThat(selected).isEqualTo(OVERFLOW_ITEM) }
        composeTestRule.onNodeWithText(OVERFLOW_ITEM).assertDoesNotExist()
    }

    private companion object {
        const val STRING_TITLE = "String title"
        const val COMPOSABLE_TITLE = "Composable title"
        const val INLINE_ACTION = "Inline action"
        const val ENABLED_ICON_DESCRIPTION = "Open"
        const val DISABLED_ICON_DESCRIPTION = "Share disabled"
        const val ENABLED_TEXT_ACTION = "Save"
        const val DISABLED_TEXT_ACTION = "Done"
        const val OVERFLOW_DESCRIPTION = "More options"
        const val OVERFLOW_ITEM = "Duplicate"
    }
}
