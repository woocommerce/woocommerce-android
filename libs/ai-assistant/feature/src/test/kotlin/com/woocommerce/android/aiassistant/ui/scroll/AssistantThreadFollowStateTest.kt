package com.woocommerce.android.aiassistant.ui.scroll

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantThreadFollowStateTest {
    @Test
    fun `given no visible lazy items, when resolving viewport follow state, then observation is not consumed`() {
        val result = resolveAssistantThreadFollowState(
            viewport = viewportState(hasVisibleItems = false),
            isProgrammaticScrollInProgress = false,
            current = followState(
                autoFollowEnabled = true,
                hasObservedNonEmptyViewport = false,
            ),
        )

        assertThat(result.autoFollowEnabled).isTrue()
        assertThat(result.hasObservedNonEmptyViewport).isFalse()
    }

    @Test
    fun `given restored viewport is off bottom, when resolving follow state, then auto follow is disabled`() {
        val result = resolveAssistantThreadFollowState(
            viewport = viewportState(isPinnedToEnd = false),
            isProgrammaticScrollInProgress = false,
            current = followState(
                autoFollowEnabled = true,
                hasObservedNonEmptyViewport = false,
            ),
        )

        assertThat(result.autoFollowEnabled).isFalse()
        assertThat(result.hasObservedNonEmptyViewport).isTrue()
    }

    @Test
    fun `given programmatic scroll is in progress, when viewport is not pinned, then auto follow stays enabled`() {
        val result = resolveAssistantThreadFollowState(
            viewport = viewportState(
                isPinnedToEnd = false,
                isScrollInProgress = true,
            ),
            isProgrammaticScrollInProgress = true,
            current = followState(
                autoFollowEnabled = true,
                hasObservedNonEmptyViewport = true,
            ),
        )

        assertThat(result.autoFollowEnabled).isTrue()
        assertThat(result.hasObservedNonEmptyViewport).isTrue()
    }

    @Test
    fun `given following and viewport shrinks off latest, when checking correction, then snap to latest`() {
        val shouldSnap = shouldSnapToLatestAfterViewportChange(
            viewport = viewportState(isPinnedToEnd = false),
            followState = followState(
                autoFollowEnabled = true,
                hasObservedNonEmptyViewport = true,
            ),
        )

        assertThat(shouldSnap).isTrue()
    }

    @Test
    fun `given restored viewport has not been observed, when checking correction, then do not snap to latest`() {
        val shouldSnap = shouldSnapToLatestAfterViewportChange(
            viewport = viewportState(isPinnedToEnd = false),
            followState = followState(
                autoFollowEnabled = true,
                hasObservedNonEmptyViewport = false,
            ),
        )

        assertThat(shouldSnap).isFalse()
    }

    @Test
    fun `given user scroll is active, when checking correction, then do not snap to latest`() {
        val shouldSnap = shouldSnapToLatestAfterViewportChange(
            viewport = viewportState(
                isPinnedToEnd = false,
                isScrollInProgress = true,
            ),
            followState = followState(
                autoFollowEnabled = true,
                hasObservedNonEmptyViewport = true,
            ),
        )

        assertThat(shouldSnap).isFalse()
    }

    private fun viewportState(
        renderedItemCount: Int = 4,
        hasVisibleItems: Boolean = true,
        isPinnedToEnd: Boolean = true,
        isScrollInProgress: Boolean = false,
    ) = AssistantThreadViewportState(
        renderedItemCount = renderedItemCount,
        hasVisibleItems = hasVisibleItems,
        isPinnedToEnd = isPinnedToEnd,
        isScrollInProgress = isScrollInProgress,
    )

    private fun followState(
        autoFollowEnabled: Boolean,
        hasObservedNonEmptyViewport: Boolean,
    ) = AssistantThreadFollowState(
        autoFollowEnabled = autoFollowEnabled,
        hasObservedNonEmptyViewport = hasObservedNonEmptyViewport,
    )
}
