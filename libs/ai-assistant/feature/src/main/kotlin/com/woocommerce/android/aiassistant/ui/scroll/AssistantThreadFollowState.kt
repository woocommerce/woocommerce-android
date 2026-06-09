package com.woocommerce.android.aiassistant.ui.scroll

internal data class AssistantThreadFollowState(
    val autoFollowEnabled: Boolean,
    val hasObservedNonEmptyViewport: Boolean,
)

internal data class AssistantThreadViewportState(
    val renderedItemCount: Int,
    val hasVisibleItems: Boolean,
    val isPinnedToEnd: Boolean,
    val isScrollInProgress: Boolean,
) {
    val hasRenderedItems: Boolean
        get() = renderedItemCount > 0
}

internal fun resolveAssistantThreadFollowState(
    viewport: AssistantThreadViewportState,
    isProgrammaticScrollInProgress: Boolean,
    current: AssistantThreadFollowState,
): AssistantThreadFollowState {
    if (!viewport.hasRenderedItems || !viewport.hasVisibleItems) {
        return current
    }

    if (!current.hasObservedNonEmptyViewport) {
        return AssistantThreadFollowState(
            autoFollowEnabled = viewport.isPinnedToEnd,
            hasObservedNonEmptyViewport = true,
        )
    }

    return AssistantThreadFollowState(
        autoFollowEnabled = when {
            viewport.isPinnedToEnd -> true
            viewport.isScrollInProgress && !isProgrammaticScrollInProgress -> false
            else -> current.autoFollowEnabled
        },
        hasObservedNonEmptyViewport = true,
    )
}

internal fun shouldSnapToLatestAfterViewportChange(
    viewport: AssistantThreadViewportState,
    followState: AssistantThreadFollowState,
): Boolean = viewport.hasRenderedItems &&
    viewport.hasVisibleItems &&
    followState.hasObservedNonEmptyViewport &&
    followState.autoFollowEnabled &&
    !viewport.isPinnedToEnd &&
    !viewport.isScrollInProgress
