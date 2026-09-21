package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooOverflowMenuTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given collapsed menu, when trigger is clicked, then content is shown`() {
        // GIVEN
        setMenuContent()
        composeTestRule.onNodeWithText(FIRST_ITEM).assertDoesNotExist()

        // WHEN
        composeTestRule.onNodeWithText(TRIGGER).performClick()

        // THEN
        composeTestRule.onNodeWithText(FIRST_ITEM).assertExists()
        composeTestRule.onNodeWithText(SECOND_ITEM).assertExists()
    }

    @Test
    fun `given expanded menu, when item is clicked, then callback runs and menu is dismissed`() {
        // GIVEN
        var selected: String? = null
        setMenuContent(onItemClick = { selected = it })
        composeTestRule.onNodeWithText(TRIGGER).performClick()

        // WHEN
        composeTestRule.onNodeWithText(SECOND_ITEM).performClick()

        // THEN
        composeTestRule.runOnIdle { assertThat(selected).isEqualTo(SECOND_ITEM) }
        composeTestRule.onNodeWithText(SECOND_ITEM).assertDoesNotExist()
    }

    @Test
    fun `given disabled item, when rendered, then it is not enabled`() {
        // GIVEN
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooOverflowMenuItem(
                    text = FIRST_ITEM,
                    onClick = {},
                    enabled = false,
                )
            }
        }

        // THEN
        composeTestRule.onNodeWithText(FIRST_ITEM).assertIsNotEnabled()
    }

    @Test
    fun `given item with trailing icon, when rendered, then trailing content is shown`() {
        // GIVEN
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooOverflowMenuItem(
                    text = FIRST_ITEM,
                    onClick = {},
                    trailingIcon = { Text(text = TRAILING_CONTENT) },
                )
            }
        }

        // THEN
        composeTestRule.onNodeWithText(FIRST_ITEM).assertExists()
        composeTestRule.onNodeWithText(TRAILING_CONTENT).assertExists()
    }

    @Test
    fun `given blank item text, when rendered, then illegal argument is thrown`() {
        assertThatIllegalArgumentException().isThrownBy {
            composeTestRule.setContent {
                WooDesignSystemTheme {
                    WooOverflowMenuItem(text = " ", onClick = {})
                }
            }
        }
    }

    private fun setMenuContent(onItemClick: (String) -> Unit = {}) {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooOverflowMenu(
                    trigger = { onClick ->
                        Text(text = TRIGGER, modifier = Modifier.clickable(onClick = onClick))
                    },
                ) { dismiss ->
                    listOf(FIRST_ITEM, SECOND_ITEM).forEach { item ->
                        WooOverflowMenuItem(
                            text = item,
                            onClick = {
                                dismiss()
                                onItemClick(item)
                            },
                            isDestructive = item == SECOND_ITEM,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val TRIGGER = "More"
        const val FIRST_ITEM = "Duplicate"
        const val SECOND_ITEM = "Delete"
        const val TRAILING_CONTENT = "Selected"
    }
}
