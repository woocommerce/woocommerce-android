package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooTooltipTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `when arrow positions are enumerated, then all twelve variants are exposed`() {
        assertThat(WooTooltipArrowPosition.entries).containsExactly(
            WooTooltipArrowPosition.TopStart,
            WooTooltipArrowPosition.TopCenter,
            WooTooltipArrowPosition.TopEnd,
            WooTooltipArrowPosition.BottomStart,
            WooTooltipArrowPosition.BottomCenter,
            WooTooltipArrowPosition.BottomEnd,
            WooTooltipArrowPosition.StartTop,
            WooTooltipArrowPosition.StartCenter,
            WooTooltipArrowPosition.StartBottom,
            WooTooltipArrowPosition.EndTop,
            WooTooltipArrowPosition.EndCenter,
            WooTooltipArrowPosition.EndBottom,
        )
    }

    @Test
    fun `given logical arrow positions, when layout is RTL, then horizontal positions mirror`() {
        assertThat(WooTooltipArrowPosition.TopStart.resolve(LayoutDirection.Rtl)).isEqualTo(
            ResolvedWooTooltipArrowPosition(
                edge = WooTooltipPhysicalEdge.Top,
                alignment = WooTooltipPhysicalAlignment.End,
            )
        )
        assertThat(WooTooltipArrowPosition.BottomEnd.resolve(LayoutDirection.Rtl)).isEqualTo(
            ResolvedWooTooltipArrowPosition(
                edge = WooTooltipPhysicalEdge.Bottom,
                alignment = WooTooltipPhysicalAlignment.Start,
            )
        )
        assertThat(WooTooltipArrowPosition.StartTop.resolve(LayoutDirection.Rtl)).isEqualTo(
            ResolvedWooTooltipArrowPosition(
                edge = WooTooltipPhysicalEdge.Right,
                alignment = WooTooltipPhysicalAlignment.Start,
            )
        )
        assertThat(WooTooltipArrowPosition.EndBottom.resolve(LayoutDirection.Rtl)).isEqualTo(
            ResolvedWooTooltipArrowPosition(
                edge = WooTooltipPhysicalEdge.Left,
                alignment = WooTooltipPhysicalAlignment.End,
            )
        )
    }

    @Test
    fun `given Figma dimensions, when arrow tips resolve, then all placement centers match`() {
        val density = Density(density = 1f, fontScale = 1f)

        FIGMA_ARROW_TIPS.forEach { expected ->
            val actualTip = wooTooltipArrowTip(
                arrowPosition = expected.position,
                size = expected.size,
                layoutDirection = LayoutDirection.Ltr,
                density = density,
                cornerRadius = FIGMA_CORNER_RADIUS,
            )

            assertThat(actualTip).isEqualTo(expected.tip)
        }
    }

    @Test
    fun `given rich tooltip, when rendered, then text appears once in semantics`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                WooTooltip(
                    title = TITLE,
                    supportingText = SUPPORTING_TEXT,
                    arrowPosition = WooTooltipArrowPosition.TopStart,
                    modifier = Modifier.width(TOOLTIP_WIDTH),
                )
            }
        }

        composeTestRule.onAllNodesWithText(TITLE, useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onAllNodesWithText(SUPPORTING_TEXT, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `given supporting text, when rendered, then tooltip expands without clipping text`() {
        composeTestRule.setContent {
            WooDesignSystemTheme {
                Column {
                    WooTooltip(
                        title = TITLE,
                        arrowPosition = WooTooltipArrowPosition.EndCenter,
                        modifier = Modifier
                            .width(TOOLTIP_WIDTH)
                            .testTag(TITLE_ONLY_TOOLTIP_TAG),
                    )
                    WooTooltip(
                        title = TITLE,
                        supportingText = LONG_SUPPORTING_TEXT,
                        arrowPosition = WooTooltipArrowPosition.EndCenter,
                        modifier = Modifier
                            .width(TOOLTIP_WIDTH)
                            .testTag(RICH_TOOLTIP_TAG),
                    )
                }
            }
        }

        val titleOnlyBounds = composeTestRule.onNodeWithTag(TITLE_ONLY_TOOLTIP_TAG).fetchSemanticsNode().boundsInRoot
        val richBounds = composeTestRule.onNodeWithTag(RICH_TOOLTIP_TAG).fetchSemanticsNode().boundsInRoot

        assertThat(richBounds.height).isGreaterThan(titleOnlyBounds.height)
        composeTestRule.onAllNodesWithText(LONG_SUPPORTING_TEXT, useUnmergedTree = true).assertCountEquals(1)
    }

    private data class ExpectedArrowTip(
        val position: WooTooltipArrowPosition,
        val size: Size,
        val tip: Offset,
    )

    private companion object {
        const val TITLE = "Title"
        const val SUPPORTING_TEXT = "Supporting text"
        const val LONG_SUPPORTING_TEXT = "Supporting information wraps across several lines without clipping."
        const val TITLE_ONLY_TOOLTIP_TAG = "title-only-tooltip"
        const val RICH_TOOLTIP_TAG = "rich-tooltip"
        val TOOLTIP_WIDTH = 200.dp
        val FIGMA_CORNER_RADIUS = 12.dp
        val TOP_BOTTOM_SIZE = Size(width = 200f, height = 122f)
        val SIDE_SIZE = Size(width = 200f, height = 112f)
        val FIGMA_ARROW_TIPS = listOf(
            ExpectedArrowTip(WooTooltipArrowPosition.TopStart, TOP_BOTTOM_SIZE, Offset(31f, 0f)),
            ExpectedArrowTip(WooTooltipArrowPosition.TopCenter, TOP_BOTTOM_SIZE, Offset(100f, 0f)),
            ExpectedArrowTip(WooTooltipArrowPosition.TopEnd, TOP_BOTTOM_SIZE, Offset(169f, 0f)),
            ExpectedArrowTip(WooTooltipArrowPosition.BottomStart, TOP_BOTTOM_SIZE, Offset(31f, 122f)),
            ExpectedArrowTip(WooTooltipArrowPosition.BottomCenter, TOP_BOTTOM_SIZE, Offset(100f, 122f)),
            ExpectedArrowTip(WooTooltipArrowPosition.BottomEnd, TOP_BOTTOM_SIZE, Offset(169f, 122f)),
            ExpectedArrowTip(WooTooltipArrowPosition.StartTop, SIDE_SIZE, Offset(0f, 31f)),
            ExpectedArrowTip(WooTooltipArrowPosition.StartCenter, SIDE_SIZE, Offset(0f, 56f)),
            ExpectedArrowTip(WooTooltipArrowPosition.StartBottom, SIDE_SIZE, Offset(0f, 81f)),
            ExpectedArrowTip(WooTooltipArrowPosition.EndTop, SIDE_SIZE, Offset(200f, 31f)),
            ExpectedArrowTip(WooTooltipArrowPosition.EndCenter, SIDE_SIZE, Offset(200f, 56f)),
            ExpectedArrowTip(WooTooltipArrowPosition.EndBottom, SIDE_SIZE, Offset(200f, 81f)),
        )
    }
}
