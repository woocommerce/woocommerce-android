package com.woocommerce.android.aiassistant.ui.scroll

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantBottomPinGeometryTest {
    @Test
    fun `given target bottom is near below viewport end, when checking pin state, then target is pinned`() {
        assertThat(
            isRenderedTargetPinnedAboveBottomContent(
                distanceFromViewportEnd = -48,
                bottomContentPaddingPx = 0,
                bottomPinThresholdPx = 48,
            )
        ).isTrue()
    }

    @Test
    fun `given target bottom is far below viewport end, when checking pin state, then target is not pinned`() {
        assertThat(
            isRenderedTargetPinnedAboveBottomContent(
                distanceFromViewportEnd = -49,
                bottomContentPaddingPx = 0,
                bottomPinThresholdPx = 48,
            )
        ).isFalse()
    }

    @Test
    fun `given target bottom is far above viewport end, when checking pin state, then target is pinned`() {
        assertThat(
            isRenderedTargetPinnedAboveBottomContent(
                distanceFromViewportEnd = 49,
                bottomContentPaddingPx = 0,
                bottomPinThresholdPx = 48,
            )
        ).isTrue()
    }

    @Test
    fun `given target bottom is under composer, when checking pin state, then target is not pinned`() {
        assertThat(
            isRenderedTargetPinnedAboveBottomContent(
                distanceFromViewportEnd = 0,
                bottomContentPaddingPx = 160,
                bottomPinThresholdPx = 48,
            )
        ).isFalse()
    }

    @Test
    fun `given target bottom reaches visible end above composer, when checking pin state, then target is pinned`() {
        assertThat(
            isRenderedTargetPinnedAboveBottomContent(
                distanceFromViewportEnd = 160,
                bottomContentPaddingPx = 160,
                bottomPinThresholdPx = 48,
            )
        ).isTrue()
    }

    @Test
    fun `given target bottom is hidden by composer, when computing reveal delta, then reveal delta is returned`() {
        assertThat(
            scrollDeltaToRevealTargetAboveBottomContent(
                distanceFromViewportEnd = 0,
                bottomContentPaddingPx = 160,
            )
        ).isEqualTo(160f)
    }

    @Test
    fun `given target bottom is already above composer, when computing reveal delta, then content does not scroll`() {
        assertThat(
            scrollDeltaToRevealTargetAboveBottomContent(
                distanceFromViewportEnd = 180,
                bottomContentPaddingPx = 160,
            )
        ).isEqualTo(0f)
    }
}
