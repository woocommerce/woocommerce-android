package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.loadWooColors
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
class WooCellTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val colors = loadWooColors(
        context = ApplicationProvider.getApplicationContext(),
        useDarkTheme = false,
    )

    @Test
    fun `given enabled cell, when style resolves, then shell and slots match live bindings`() {
        val style = wooCellStyle(enabled = true, colors = colors)

        assertThat(style.containerColor).isEqualTo(colors.surface.bright)
        assertThat(style.slotContentColor).isEqualTo(colors.surface.onVariant)
    }

    @Test
    fun `given disabled cell, when style resolves, then slots use lowest variant`() {
        val style = wooCellStyle(enabled = false, colors = colors)

        assertThat(style.containerColor).isEqualTo(colors.surface.bright)
        assertThat(style.slotContentColor).isEqualTo(colors.surface.onVariantLowest)
    }

    @Test
    fun `when cell is rendered, then shell has 24dp padding on every side`() {
        // GIVEN
        var expectedPaddingPx = 0f
        composeTestRule.setContent {
            WooDesignSystemTheme {
                expectedPaddingPx = with(LocalDensity.current) { EXPECTED_CELL_PADDING.toPx() }
                WooCell(
                    title = CELL_TITLE,
                    modifier = Modifier
                        .requiredWidth(CONSTRAINED_CELL_WIDTH)
                        .testTag(CELL_TAG),
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(LEADING_CONTENT_SIZE)
                                .testTag(LEADING_CONTENT_TAG),
                        )
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .size(TRAILING_CONTENT_SIZE)
                                .testTag(TRAILING_CONTENT_TAG),
                        )
                    },
                )
            }
        }

        // THEN
        val cellBounds = boundsForTag(CELL_TAG)
        val leadingBounds = boundsForTag(LEADING_CONTENT_TAG, useUnmergedTree = true)
        val trailingBounds = boundsForTag(TRAILING_CONTENT_TAG, useUnmergedTree = true)

        assertThat(leadingBounds.left - cellBounds.left)
            .isCloseTo(expectedPaddingPx, within(POSITION_TOLERANCE))
        assertThat(cellBounds.right - trailingBounds.right)
            .isCloseTo(expectedPaddingPx, within(POSITION_TOLERANCE))
        assertThat(leadingBounds.top - cellBounds.top)
            .isCloseTo(expectedPaddingPx, within(POSITION_TOLERANCE))
        assertThat(cellBounds.bottom - leadingBounds.bottom)
            .isCloseTo(expectedPaddingPx, within(POSITION_TOLERANCE))
    }

    @Test
    fun `given constrained RTL cell, when rendered, then text fills its space and remains on one line`() {
        // GIVEN
        var slotSpacingPx = 0f
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                WooDesignSystemTheme {
                    slotSpacingPx = with(LocalDensity.current) { WooTheme.spacing.space5.toPx() }
                    WooCell(
                        title = ARABIC_TITLE,
                        description = ARABIC_DESCRIPTION,
                        onClick = {},
                        modifier = Modifier.requiredWidth(CONSTRAINED_CELL_WIDTH),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(LEADING_CONTENT_SIZE)
                                    .testTag(LEADING_CONTENT_TAG),
                            )
                        },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(TRAILING_CONTENT_SIZE)
                                    .testTag(TRAILING_CONTENT_TAG),
                            )
                        },
                    )
                }
            }
        }

        // THEN
        val titleBounds = textBounds(ARABIC_TITLE)
        val descriptionBounds = textBounds(ARABIC_DESCRIPTION)
        val leadingBounds = composeTestRule
            .onNodeWithTag(LEADING_CONTENT_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val trailingBounds = composeTestRule
            .onNodeWithTag(TRAILING_CONTENT_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val expectedTextLeft = trailingBounds.right + slotSpacingPx
        val expectedTextRight = leadingBounds.left - slotSpacingPx

        assertThat(titleBounds.left.roundToInt()).isEqualTo(expectedTextLeft.roundToInt())
        assertThat(titleBounds.right.roundToInt()).isEqualTo(expectedTextRight.roundToInt())
        assertThat(descriptionBounds.left.roundToInt()).isEqualTo(expectedTextLeft.roundToInt())
        assertThat(descriptionBounds.right.roundToInt()).isEqualTo(expectedTextRight.roundToInt())
        assertThat(textLayoutResult(ARABIC_TITLE).lineCount).isEqualTo(1)
        assertThat(textLayoutResult(ARABIC_DESCRIPTION).lineCount).isEqualTo(1)
    }

    private fun boundsForTag(tag: String, useUnmergedTree: Boolean = false): Rect = composeTestRule
        .onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
        .fetchSemanticsNode()
        .boundsInRoot

    private fun textBounds(text: String): Rect = composeTestRule
        .onNodeWithText(text, useUnmergedTree = true)
        .fetchSemanticsNode()
        .boundsInRoot

    private fun textLayoutResult(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val action = composeTestRule
            .onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action

        composeTestRule.runOnIdle {
            assertThat(action?.invoke(results)).isTrue()
        }
        return results.single()
    }

    private companion object {
        const val ARABIC_TITLE = "الاشتراكات"
        const val ARABIC_DESCRIPTION = "إدارة اشتراكك"
        const val CELL_TITLE = "Cell title"
        const val CELL_TAG = "cell"
        const val LEADING_CONTENT_TAG = "leading-content"
        const val TRAILING_CONTENT_TAG = "trailing-content"
        const val POSITION_TOLERANCE = 1f
        val CONSTRAINED_CELL_WIDTH = 320.dp
        val EXPECTED_CELL_PADDING = 24.dp
        val LEADING_CONTENT_SIZE = 44.dp
        val TRAILING_CONTENT_SIZE = 18.dp
    }
}
