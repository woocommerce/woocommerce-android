package com.woocommerce.android.aiassistant.ui.scroll

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.aiassistant.ui.AssistantUiMessage
import com.woocommerce.android.aiassistant.ui.AssistantUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun rememberAssistantScrollController(
    state: AssistantUiState,
    visibleMessages: List<AssistantUiMessage>,
    showTypingIndicator: Boolean,
    bottomContentPadding: Dp,
): AssistantScrollController {
    val listState = rememberLazyListState()
    val bottomPinThresholdPx = with(LocalDensity.current) { BOTTOM_PIN_THRESHOLD_DP.roundToPx() }
    val bottomContentPaddingPx = with(LocalDensity.current) { bottomContentPadding.roundToPx() }
    val scrollSignal = visibleMessages.toThreadScrollSignal(
        state = state,
        showTypingIndicator = showTypingIndicator,
    )
    val viewportState = rememberAssistantThreadViewport(
        listState = listState,
        renderedItemCount = scrollSignal.renderedItemCount,
        bottomContentPaddingPx = bottomContentPaddingPx,
        bottomPinThresholdPx = bottomPinThresholdPx,
    )

    var previousScrollSignal by remember { mutableStateOf<AssistantThreadScrollSignal?>(null) }
    val autoFollowEnabled = rememberSaveable { mutableStateOf(true) }
    val hasNewerContentBelow = remember { mutableStateOf(false) }
    var isProgrammaticScrollInProgress by remember { mutableStateOf(false) }
    var hasObservedNonEmptyViewport by remember { mutableStateOf(false) }

    suspend fun scrollToLatest(animated: Boolean) {
        val targetIndex = scrollSignal.renderedItemCount - 1
        if (targetIndex < 0) return

        isProgrammaticScrollInProgress = true
        try {
            if (animated) {
                listState.animateScrollToItem(targetIndex)
            } else {
                listState.scrollToItem(targetIndex)
            }
            listState.revealRenderedTargetAboveBottomContent(
                targetIndex = targetIndex,
                bottomContentPaddingPx = bottomContentPaddingPx,
                animated = animated,
            )
        } finally {
            isProgrammaticScrollInProgress = false
        }
    }

    LaunchedEffect(scrollSignal) {
        val decision = decideAssistantThreadScroll(
            previous = previousScrollSignal,
            current = scrollSignal,
            autoFollowEnabled = autoFollowEnabled.value,
        )
        previousScrollSignal = scrollSignal

        when (decision) {
            AssistantThreadScrollDecision.None -> Unit
            AssistantThreadScrollDecision.AnimateToLatest -> scrollToLatest(animated = true)
            AssistantThreadScrollDecision.SnapToLatest -> scrollToLatest(animated = false)
        }
    }

    LaunchedEffect(
        viewportState.value,
        scrollSignal.renderedItemCount,
    ) {
        val viewport = viewportState.value
        val currentFollowState = AssistantThreadFollowState(
            autoFollowEnabled = autoFollowEnabled.value,
            hasObservedNonEmptyViewport = hasObservedNonEmptyViewport,
        )
        val followState = resolveAssistantThreadFollowState(
            viewport = viewport,
            isProgrammaticScrollInProgress = isProgrammaticScrollInProgress,
            current = currentFollowState,
        )
        autoFollowEnabled.value = followState.autoFollowEnabled
        hasObservedNonEmptyViewport = followState.hasObservedNonEmptyViewport
    }

    LaunchedEffect(
        viewportState.value,
        scrollSignal.renderedItemCount,
        autoFollowEnabled.value,
    ) {
        val viewport = viewportState.value
        hasNewerContentBelow.value = viewport.hasVisibleItems &&
            !autoFollowEnabled.value &&
            scrollSignal.renderedItemCount > 0 &&
            !viewport.isPinnedToEnd
    }

    LaunchedEffect(
        viewportState.value,
        autoFollowEnabled.value,
        hasObservedNonEmptyViewport,
        scrollSignal.renderedItemCount,
    ) {
        val viewport = viewportState.value
        val followState = AssistantThreadFollowState(
            autoFollowEnabled = autoFollowEnabled.value,
            hasObservedNonEmptyViewport = hasObservedNonEmptyViewport,
        )
        if (
            shouldSnapToLatestAfterViewportChange(
                viewport = viewport,
                followState = followState,
            )
        ) {
            scrollToLatest(animated = false)
        }
    }

    return AssistantScrollController(
        listState = listState,
        autoFollowEnabled = autoFollowEnabled,
        hasNewerContentBelow = hasNewerContentBelow,
        onUserMessageSubmitted = { autoFollowEnabled.value = true },
        onJumpToLatestClicked = {
            autoFollowEnabled.value = true
            scrollToLatest(animated = true)
        },
    )
}

internal class AssistantScrollController(
    val listState: LazyListState,
    val autoFollowEnabled: State<Boolean>,
    val hasNewerContentBelow: State<Boolean>,
    val onUserMessageSubmitted: () -> Unit,
    val onJumpToLatestClicked: suspend () -> Unit,
)

@Composable
private fun rememberAssistantThreadViewport(
    listState: LazyListState,
    renderedItemCount: Int,
    bottomContentPaddingPx: Int,
    bottomPinThresholdPx: Int,
): State<AssistantThreadViewportState> {
    return produceState(
        initialValue = listState.toAssistantThreadViewportState(
            renderedItemCount = renderedItemCount,
            bottomContentPaddingPx = bottomContentPaddingPx,
            bottomPinThresholdPx = bottomPinThresholdPx,
        ),
        listState,
        renderedItemCount,
        bottomContentPaddingPx,
        bottomPinThresholdPx,
    ) {
        snapshotFlow {
            listState.toAssistantThreadViewportState(
                renderedItemCount = renderedItemCount,
                bottomContentPaddingPx = bottomContentPaddingPx,
                bottomPinThresholdPx = bottomPinThresholdPx,
            )
        }
            .distinctUntilChanged()
            .collectLatest { value = it }
    }
}

private fun LazyListState.toAssistantThreadViewportState(
    renderedItemCount: Int,
    bottomContentPaddingPx: Int,
    bottomPinThresholdPx: Int,
) = AssistantThreadViewportState(
    renderedItemCount = renderedItemCount,
    isScrollInProgress = isScrollInProgress,
    hasVisibleItems = layoutInfo.visibleItemsInfo.isNotEmpty(),
    isPinnedToEnd = isRenderedEndPinnedAboveBottomContent(
        renderedItemCount = renderedItemCount,
        bottomContentPaddingPx = bottomContentPaddingPx,
        bottomPinThresholdPx = bottomPinThresholdPx,
    ),
)

private fun LazyListState.isRenderedEndPinnedAboveBottomContent(
    renderedItemCount: Int,
    bottomContentPaddingPx: Int,
    bottomPinThresholdPx: Int,
): Boolean {
    if (renderedItemCount == 0) return true

    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return false
    val targetIndex = renderedItemCount - 1
    if (lastVisibleItem.index != targetIndex) return false

    val distanceFromViewportEnd = layoutInfo.viewportEndOffset - (lastVisibleItem.offset + lastVisibleItem.size)
    return isRenderedTargetPinnedAboveBottomContent(
        distanceFromViewportEnd = distanceFromViewportEnd,
        bottomContentPaddingPx = bottomContentPaddingPx,
        bottomPinThresholdPx = bottomPinThresholdPx,
    )
}

private suspend fun LazyListState.revealRenderedTargetAboveBottomContent(
    targetIndex: Int,
    bottomContentPaddingPx: Int,
    animated: Boolean,
) {
    repeat(REVEAL_SCROLL_ADJUSTMENT_PASSES) {
        withFrameNanos { }
        val distanceFromViewportEnd = distanceFromRenderedTargetToViewportEnd(targetIndex) ?: return
        val scrollDelta = scrollDeltaToRevealTargetAboveBottomContent(
            distanceFromViewportEnd = distanceFromViewportEnd,
            bottomContentPaddingPx = bottomContentPaddingPx,
        )
        if (scrollDelta <= 0f) return

        if (animated) {
            animateScrollBy(scrollDelta)
        } else {
            scrollBy(scrollDelta)
        }
    }
}

private fun LazyListState.distanceFromRenderedTargetToViewportEnd(targetIndex: Int): Int? {
    val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex } ?: return null
    return layoutInfo.viewportEndOffset - (targetItem.offset + targetItem.size)
}

private const val REVEAL_SCROLL_ADJUSTMENT_PASSES = 2
private val BOTTOM_PIN_THRESHOLD_DP = 48.dp
