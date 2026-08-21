package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooTooltipPositionProviderTest {
    @Test
    fun `given equal vertical room, when placement is automatic, then below wins the tie`() {
        val result = layout(anchor = IntRect(190, 390, 210, 410))

        assertThat(result.side).isEqualTo(WooTooltipPhysicalSide.Below)
        assertThat(result.arrowEdge).isEqualTo(WooTooltipPhysicalEdge.Top)
        assertThat(result.offset.y).isEqualTo(414)
    }

    @Test
    fun `given more room above, when placement is automatic, then above is selected`() {
        val result = layout(anchor = IntRect(190, 650, 210, 670))

        assertThat(result.side).isEqualTo(WooTooltipPhysicalSide.Above)
        assertThat(result.arrowEdge).isEqualTo(WooTooltipPhysicalEdge.Bottom)
        assertThat(result.offset.y).isEqualTo(546)
    }

    @Test
    fun `given a usable preferred side, when laid out, then it is honored`() {
        val result = layout(
            anchor = IntRect(190, 300, 210, 320),
            preferred = WooTooltipPlacement.Above,
        )

        assertThat(result.side).isEqualTo(WooTooltipPhysicalSide.Above)
    }

    @Test
    fun `given a cramped preferred side, when the opposite fits, then placement flips`() {
        val result = layout(
            anchor = IntRect(190, 20, 210, 40),
            preferred = WooTooltipPlacement.Above,
        )

        assertThat(result.side).isEqualTo(WooTooltipPhysicalSide.Below)
    }

    @Test
    fun `given neither preferred side fits, when laid out, then it still flips to the opposite and clamps`() {
        val result = layout(
            anchor = IntRect(190, 80, 210, 100),
            window = IntSize(400, 150),
            popup = IntSize(200, 100),
            preferred = WooTooltipPlacement.Above,
        )

        assertThat(result.side).isEqualTo(WooTooltipPhysicalSide.Below)
        assertThat(result.offset.y).isEqualTo(42)
    }

    @Test
    fun `given logical side preferences, when direction changes, then start and end mirror`() {
        val ltr = layout(
            anchor = IntRect(160, 390, 180, 410),
            preferred = WooTooltipPlacement.Start,
            direction = LayoutDirection.Ltr,
            popup = IntSize(80, 60),
        )
        val rtl = layout(
            anchor = IntRect(160, 390, 180, 410),
            preferred = WooTooltipPlacement.Start,
            direction = LayoutDirection.Rtl,
            popup = IntSize(80, 60),
        )

        assertThat(ltr.side).isEqualTo(WooTooltipPhysicalSide.Left)
        assertThat(rtl.side).isEqualTo(WooTooltipPhysicalSide.Right)
    }

    @Test
    fun `given an anchor near a corner, when popup is clamped, then arrow keeps targeting it safely`() {
        val result = layout(
            anchor = IntRect(0, 100, 20, 120),
            preferred = WooTooltipPlacement.Below,
        )

        assertThat(result.offset).isEqualTo(IntOffset(8, 124))
        assertThat(result.arrowCenter).isEqualTo(23f)
        assertThat(result.maxBubbleWidth).isEqualTo(200)
    }

    @Test
    fun `given side placement with a short title, when measured at 60dp high, then full arrow geometry is retained`() {
        val result = layout(
            anchor = IntRect(120, 390, 140, 410),
            preferred = WooTooltipPlacement.End,
            popup = IntSize(80, 60),
        )
        val geometry = checkNotNull(
            wooTooltipGeometry(
                edge = result.arrowEdge,
                desiredArrowCenter = result.arrowCenter,
                size = androidx.compose.ui.geometry.Size(80f, 60f),
                tokens = TOKENS.geometry,
            )
        )

        assertThat(result.side).isEqualTo(WooTooltipPhysicalSide.Right)
        assertThat(result.arrowCenter).isEqualTo(30f)
        assertThat(geometry.scale).isEqualTo(1f)
    }

    @Test
    fun `given a constrained side, when corrective width is applied, then layout converges in one remeasurement`() {
        val first = layout(
            anchor = IntRect(190, 390, 210, 410),
            window = IntSize(310, 800),
            popup = IntSize(200, 60),
            preferred = WooTooltipPlacement.End,
        )
        val second = layout(
            anchor = IntRect(190, 390, 210, 410),
            window = IntSize(310, 800),
            popup = IntSize(first.maxBubbleWidth, 60),
            preferred = WooTooltipPlacement.End,
        )

        assertThat(first.maxBubbleWidth).isEqualTo(88)
        assertThat(second.side).isEqualTo(first.side)
        assertThat(second.maxBubbleWidth).isEqualTo(first.maxBubbleWidth)
    }

    @Test
    fun `given anchor bounds, when visibility is checked, then partial intersection shows and full exit hides`() {
        val window = IntSize(400, 800)

        assertThat(isWooTooltipAnchorVisible(Rect(-10f, 100f, 10f, 120f), window)).isTrue()
        assertThat(isWooTooltipAnchorVisible(Rect(-20f, 100f, -1f, 120f), window)).isFalse()
        assertThat(isWooTooltipAnchorVisible(Rect(20f, 801f, 40f, 821f), window)).isFalse()
    }

    @Test
    fun `given a moving anchor, when layout recalculates, then popup and continuous arrow target follow it`() {
        val first = layout(anchor = IntRect(20, 100, 40, 120))
        val moved = layout(anchor = IntRect(80, 100, 100, 120))
        val centered = layout(anchor = IntRect(190, 100, 210, 120))

        assertThat(moved.offset.x).isEqualTo(first.offset.x)
        assertThat(first.arrowCenter).isEqualTo(23f)
        assertThat(moved.arrowCenter).isGreaterThan(first.arrowCenter)
        assertThat(centered.offset.x).isGreaterThan(moved.offset.x)
        assertThat(centered.arrowCenter).isEqualTo(100f)
    }

    private fun layout(
        anchor: IntRect,
        window: IntSize = IntSize(400, 800),
        popup: IntSize = IntSize(200, 100),
        preferred: WooTooltipPlacement? = null,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ): WooTooltipLayoutResult = calculateWooTooltipLayout(
        input = WooTooltipLayoutInput(
            anchorBounds = anchor,
            windowSize = window,
            popupContentSize = popup,
            layoutDirection = direction,
            preferredPlacement = preferred,
        ),
        tokens = TOKENS,
    )

    private companion object {
        val TOKENS = WooTooltipLayoutTokens(
            windowMargin = 8,
            anchorGap = 4,
            maxBubbleWidth = 200,
            minSideWidth = 80,
            geometry = WooTooltipGeometryTokens(12f, 11f, 10f),
        )
    }
}
