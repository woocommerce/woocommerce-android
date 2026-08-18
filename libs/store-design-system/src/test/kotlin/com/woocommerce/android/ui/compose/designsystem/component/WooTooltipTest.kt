package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
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
    fun `given Figma dimensions, when geometry resolves, then all placement centers match`() {
        FIGMA_ARROW_TIPS.forEach { expected ->
            val geometry = geometryFor(
                position = expected.position,
                size = expected.size,
            )

            assertThat(geometry.arrowTip).isEqualTo(expected.tip)
        }
    }

    @Test
    fun `given normal dimensions, when geometry resolves, then requested tokens remain unscaled`() {
        DENSITIES.forEach { density ->
            WooTooltipArrowPosition.entries.forEach { position ->
                LayoutDirection.entries.forEach { layoutDirection ->
                    val edge = position.resolve(layoutDirection).edge
                    val size = figmaSize(edge = edge, density = density.density)
                    val geometry = geometryFor(position, size, layoutDirection, density)
                    val description = "$position at $size in $layoutDirection with ${density.density} density"

                    assertThat(geometry.scale).describedAs(description).isEqualTo(1f)
                    assertThat(geometry.arrowDepth)
                        .describedAs(description)
                        .isCloseTo(10f * density.density, within(TOLERANCE))
                    assertThat(geometry.arrowHalfBase)
                        .describedAs(description)
                        .isCloseTo(11f * density.density, within(TOLERANCE))
                    assertThat(geometry.cornerRadius)
                        .describedAs(description)
                        .isCloseTo(12f * density.density, within(TOLERANCE))
                    assertGeometryInvariants(geometry, size, description)
                }
            }
        }
    }

    @Test
    fun `given natural footprint thresholds, when geometry resolves, then it scales uniformly`() {
        DENSITIES.forEach { density ->
            WooTooltipArrowPosition.entries.forEach { position ->
                LayoutDirection.entries.forEach { layoutDirection ->
                    val edge = position.resolve(layoutDirection).edge

                    THRESHOLD_CASES.forEach { threshold ->
                        val size = sizeForEdge(
                            edge = edge,
                            edgeLength = threshold.edgeLength * density.density,
                            containerLength = threshold.containerLength * density.density,
                        )
                        val geometry = geometryFor(position, size, layoutDirection, density)
                        val expectedScale = minOf(
                            1f,
                            threshold.edgeLength / NATURAL_EDGE_LENGTH,
                            threshold.containerLength / NATURAL_CONTAINER_LENGTH,
                        )
                        val description =
                            "$position at $size in $layoutDirection with ${density.density} density"

                        assertThat(geometry.scale)
                            .describedAs(description)
                            .isCloseTo(expectedScale, within(TOLERANCE))
                        assertThat(geometry.cornerRadius / (12f * density.density))
                            .describedAs(description)
                            .isCloseTo(geometry.scale, within(TOLERANCE))
                        assertThat(geometry.arrowHalfBase / (11f * density.density))
                            .describedAs(description)
                            .isCloseTo(geometry.scale, within(TOLERANCE))
                        assertThat(geometry.arrowDepth / (10f * density.density))
                            .describedAs(description)
                            .isCloseTo(geometry.scale, within(TOLERANCE))
                        assertGeometryInvariants(geometry, size, description)
                    }
                }
            }
        }
    }

    @Test
    fun `given representative axis lengths, then physical start center and end never reverse`() {
        DENSITIES.forEach { density ->
            LayoutDirection.entries.forEach { layoutDirection ->
                ORDERING_EDGE_LENGTHS.forEach { edgeLength ->
                    WooTooltipPhysicalEdge.entries.forEach { edge ->
                        val size = sizeForEdge(
                            edge = edge,
                            edgeLength = edgeLength * density.density,
                            containerLength = ORDERING_CONTAINER_LENGTH * density.density,
                        )
                        val centers = WooTooltipArrowPosition.entries
                            .filter { it.resolve(layoutDirection).edge == edge }
                            .associate { position ->
                                val alignment = position.resolve(layoutDirection).alignment
                                val geometry = geometryFor(position, size, layoutDirection, density)
                                alignment to geometry.arrowCenterAlongEdge()
                            }
                        val description =
                            "$edge at ${edgeLength}dp in $layoutDirection with ${density.density} density"

                        assertThat(centers.getValue(WooTooltipPhysicalAlignment.Start))
                            .describedAs(description)
                            .isLessThanOrEqualTo(centers.getValue(WooTooltipPhysicalAlignment.Center))
                        assertThat(centers.getValue(WooTooltipPhysicalAlignment.Center))
                            .describedAs(description)
                            .isLessThanOrEqualTo(centers.getValue(WooTooltipPhysicalAlignment.End))
                    }
                }
            }
        }
    }

    @Test
    fun `given 16 by 100 top and bottom tooltips, then arrow bases stay off rounded wings`() {
        assertConstrainedGeometry(
            size = Size(width = 16f, height = 100f),
            positions = TOP_BOTTOM_POSITIONS,
        )
    }

    @Test
    fun `given 100 by 16 side tooltips, then arrow bases stay off rounded wings`() {
        assertConstrainedGeometry(
            size = Size(width = 100f, height = 16f),
            positions = SIDE_POSITIONS,
        )
    }

    @Test
    fun `given 16 by 16 tooltips, then arrow and body keep a positive throat`() {
        assertConstrainedGeometry(
            size = Size(width = 16f, height = 16f),
            positions = WooTooltipArrowPosition.entries,
        )
    }

    @Test
    fun `given small positive sizes, when outlines are created, then every path stays within its bounds`() {
        SMALL_POSITIVE_SIZES.forEach { size ->
            WooTooltipArrowPosition.entries.forEach { position ->
                LayoutDirection.entries.forEach { layoutDirection ->
                    val description = "$position at $size in $layoutDirection"
                    val geometry = geometryFor(position, size, layoutDirection)

                    assertGeometryInvariants(geometry, size, description)
                    assertOutlineInBounds(position, size, layoutDirection, description)
                }
            }
        }
    }

    @Test
    fun `given invalid sizes, when outlines are created, then path operations are skipped safely`() {
        val shape = WooTooltipShape(
            arrowPosition = WooTooltipArrowPosition.TopCenter,
            cornerRadius = FIGMA_CORNER_RADIUS,
        )

        INVALID_SIZES.forEach { size ->
            val geometry = wooTooltipGeometry(
                arrowPosition = WooTooltipArrowPosition.TopCenter,
                size = size,
                layoutDirection = LayoutDirection.Ltr,
                density = DEFAULT_DENSITY,
                cornerRadius = FIGMA_CORNER_RADIUS,
            )
            val outline = shape.createOutline(size, LayoutDirection.Ltr, DEFAULT_DENSITY)

            assertThat(geometry).describedAs("geometry for $size").isNull()
            assertThat(outline).describedAs("outline for $size").isInstanceOf(Outline.Rectangle::class.java)
            assertThat((outline as Outline.Rectangle).rect).isEqualTo(Rect(0f, 0f, 0f, 0f))
        }
    }

    @Test
    fun `given equivalent parameters, when shapes are recreated, then they are value equal`() {
        assertThat(
            WooTooltipShape(
                arrowPosition = WooTooltipArrowPosition.TopStart,
                cornerRadius = FIGMA_CORNER_RADIUS,
            )
        ).isEqualTo(
            WooTooltipShape(
                arrowPosition = WooTooltipArrowPosition.TopStart,
                cornerRadius = FIGMA_CORNER_RADIUS,
            )
        )
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
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        val textLayoutResultAction = composeTestRule
            .onNodeWithText(LONG_SUPPORTING_TEXT, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action

        assertThat(richBounds.height).isGreaterThan(titleOnlyBounds.height)
        composeTestRule.onAllNodesWithText(LONG_SUPPORTING_TEXT, useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.runOnIdle {
            assertThat(textLayoutResultAction?.invoke(textLayoutResults)).isTrue()
            val textLayoutResult = textLayoutResults.single()
            assertThat(textLayoutResult.hasVisualOverflow)
                .describedAs(
                    "width overflow=${textLayoutResult.didOverflowWidth}, " +
                        "height overflow=${textLayoutResult.didOverflowHeight}, " +
                        "lines=${textLayoutResult.lineCount}, size=${textLayoutResult.size}"
                )
                .isFalse()
        }
    }

    private fun assertConstrainedGeometry(
        size: Size,
        positions: List<WooTooltipArrowPosition>,
    ) {
        positions.forEach { position ->
            LayoutDirection.entries.forEach { layoutDirection ->
                val description = "$position at $size in $layoutDirection"
                val geometry = geometryFor(position, size, layoutDirection)

                assertGeometryInvariants(geometry, size, description)
                assertOutlineInBounds(position, size, layoutDirection, description)
            }
        }
    }

    private fun assertGeometryInvariants(
        geometry: WooTooltipGeometry,
        size: Size,
        description: String,
    ) {
        val baseInterval = geometry.baseInterval()
        val edgeLength = geometry.edgeLength(size)
        val straightEdge = geometry.cornerRadius..(edgeLength - geometry.cornerRadius)

        assertThat(geometry.scale).describedAs(description).isGreaterThan(0f).isLessThanOrEqualTo(1f)
        assertThat(geometry.arrowDepth).describedAs(description).isGreaterThan(0f)
        assertThat(geometry.arrowHalfBase).describedAs(description).isGreaterThan(0f)
        assertThat(geometry.cornerRadius).describedAs(description).isGreaterThan(0f)
        assertThat(geometry.cornerRadius)
            .describedAs(description)
            .isLessThanOrEqualTo(geometry.bodyBounds.width / 2f)
        assertThat(geometry.cornerRadius)
            .describedAs(description)
            .isLessThanOrEqualTo(geometry.bodyBounds.height / 2f)
        assertThat(baseInterval.start).describedAs(description).isGreaterThanOrEqualTo(straightEdge.start)
        assertThat(baseInterval.endInclusive).describedAs(description).isLessThanOrEqualTo(straightEdge.endInclusive)
        assertThat(baseInterval.endInclusive - baseInterval.start)
            .describedAs(description)
            .isGreaterThan(0f)
            .isCloseTo(geometry.arrowHalfBase * 2f, within(TOLERANCE))
        assertThat(geometry.actualArrowDepth())
            .describedAs(description)
            .isCloseTo(geometry.arrowDepth, within(TOLERANCE))
        assertPointsInBounds(geometry, size, description)
    }

    private fun assertPointsInBounds(
        geometry: WooTooltipGeometry,
        size: Size,
        description: String,
    ) {
        listOf(geometry.arrowTip, geometry.arrowBaseStart, geometry.arrowBaseEnd).forEach { point ->
            assertThat(point.x).describedAs(description).isBetween(0f, size.width)
            assertThat(point.y).describedAs(description).isBetween(0f, size.height)
        }
        assertThat(geometry.bodyBounds.left).describedAs(description).isGreaterThanOrEqualTo(0f)
        assertThat(geometry.bodyBounds.top).describedAs(description).isGreaterThanOrEqualTo(0f)
        assertThat(geometry.bodyBounds.right).describedAs(description).isLessThanOrEqualTo(size.width)
        assertThat(geometry.bodyBounds.bottom).describedAs(description).isLessThanOrEqualTo(size.height)
    }

    private fun assertOutlineInBounds(
        position: WooTooltipArrowPosition,
        size: Size,
        layoutDirection: LayoutDirection,
        description: String,
    ) {
        val outline = WooTooltipShape(position, FIGMA_CORNER_RADIUS).createOutline(
            size = size,
            layoutDirection = layoutDirection,
            density = DEFAULT_DENSITY,
        )

        assertThat(outline).describedAs(description).isInstanceOf(Outline.Generic::class.java)
        val bounds = (outline as Outline.Generic).path.getBounds()
        assertThat(bounds.left).describedAs(description).isGreaterThanOrEqualTo(0f)
        assertThat(bounds.top).describedAs(description).isGreaterThanOrEqualTo(0f)
        assertThat(bounds.right).describedAs(description).isLessThanOrEqualTo(size.width)
        assertThat(bounds.bottom).describedAs(description).isLessThanOrEqualTo(size.height)
        assertThat(bounds.width).describedAs(description).isGreaterThan(0f)
        assertThat(bounds.height).describedAs(description).isGreaterThan(0f)
    }

    private fun geometryFor(
        position: WooTooltipArrowPosition,
        size: Size,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        density: Density = DEFAULT_DENSITY,
    ): WooTooltipGeometry = checkNotNull(
        wooTooltipGeometry(
            arrowPosition = position,
            size = size,
            layoutDirection = layoutDirection,
            density = density,
            cornerRadius = FIGMA_CORNER_RADIUS,
        )
    )

    private fun WooTooltipGeometry.baseInterval(): ClosedFloatingPointRange<Float> = when (edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> arrowBaseStart.x..arrowBaseEnd.x

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> arrowBaseStart.y..arrowBaseEnd.y
    }

    private fun WooTooltipGeometry.edgeLength(size: Size): Float = when (edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> size.width

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> size.height
    }

    private fun WooTooltipGeometry.actualArrowDepth(): Float = when (edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> kotlin.math.abs(arrowTip.y - arrowBaseStart.y)

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> kotlin.math.abs(arrowTip.x - arrowBaseStart.x)
    }

    private fun WooTooltipGeometry.arrowCenterAlongEdge(): Float = when (edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> arrowTip.x

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> arrowTip.y
    }

    private fun sizeForEdge(
        edge: WooTooltipPhysicalEdge,
        edgeLength: Float,
        containerLength: Float,
    ): Size = when (edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> Size(width = edgeLength, height = containerLength)

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> Size(width = containerLength, height = edgeLength)
    }

    private fun figmaSize(edge: WooTooltipPhysicalEdge, density: Float): Size = when (edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> TOP_BOTTOM_SIZE * density

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> SIDE_SIZE * density
    }

    private data class ExpectedArrowTip(
        val position: WooTooltipArrowPosition,
        val size: Size,
        val tip: Offset,
    )

    private data class ThresholdCase(
        val edgeLength: Float,
        val containerLength: Float,
    )

    private companion object {
        const val TITLE = "Title"
        const val SUPPORTING_TEXT = "Supporting text"
        const val LONG_SUPPORTING_TEXT =
            "Supporting information wraps across several lines without clipping, even when the message contains " +
                "enough detail to exceed the available width and adapt to a caller-controlled tooltip size."
        const val TITLE_ONLY_TOOLTIP_TAG = "title-only-tooltip"
        const val RICH_TOOLTIP_TAG = "rich-tooltip"
        const val NATURAL_EDGE_LENGTH = 62f
        const val NATURAL_CONTAINER_LENGTH = 34f
        const val ORDERING_CONTAINER_LENGTH = 100f
        const val TOLERANCE = 0.0001f
        val TOOLTIP_WIDTH = 200.dp
        val FIGMA_CORNER_RADIUS = 12.dp
        val DEFAULT_DENSITY = Density(density = 1f, fontScale = 1f)
        val DENSITIES = listOf(DEFAULT_DENSITY, Density(density = 2.625f, fontScale = 1f))
        val TOP_BOTTOM_SIZE = Size(width = 200f, height = 122f)
        val SIDE_SIZE = Size(width = 200f, height = 112f)
        val TOP_BOTTOM_POSITIONS = listOf(
            WooTooltipArrowPosition.TopStart,
            WooTooltipArrowPosition.TopCenter,
            WooTooltipArrowPosition.TopEnd,
            WooTooltipArrowPosition.BottomStart,
            WooTooltipArrowPosition.BottomCenter,
            WooTooltipArrowPosition.BottomEnd,
        )
        val SIDE_POSITIONS = listOf(
            WooTooltipArrowPosition.StartTop,
            WooTooltipArrowPosition.StartCenter,
            WooTooltipArrowPosition.StartBottom,
            WooTooltipArrowPosition.EndTop,
            WooTooltipArrowPosition.EndCenter,
            WooTooltipArrowPosition.EndBottom,
        )
        val THRESHOLD_CASES = listOf(
            ThresholdCase(edgeLength = 61.5f, containerLength = 34.5f),
            ThresholdCase(edgeLength = 62f, containerLength = 34f),
            ThresholdCase(edgeLength = 62.5f, containerLength = 33.5f),
            ThresholdCase(edgeLength = 62.5f, containerLength = 34.5f),
        )
        val ORDERING_EDGE_LENGTHS = listOf(46f, 50f, 60f, 62f, SIDE_SIZE.height, TOP_BOTTOM_SIZE.width)
        val SMALL_POSITIVE_SIZES = listOf(
            Size(width = 0.5f, height = 0.5f),
            Size(width = 1f, height = 1f),
            Size(width = 16f, height = 100f),
            Size(width = 100f, height = 16f),
            Size(width = 16f, height = 16f),
        )
        val INVALID_SIZES = listOf(
            Size.Zero,
            Size(width = 0f, height = 10f),
            Size(width = 10f, height = 0f),
            Size(width = -1f, height = 10f),
            Size.Unspecified,
            Size(width = Float.NaN, height = 10f),
            Size(width = 10f, height = Float.POSITIVE_INFINITY),
        )
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
