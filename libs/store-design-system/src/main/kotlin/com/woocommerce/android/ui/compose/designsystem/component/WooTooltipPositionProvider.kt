package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.max
import kotlin.math.min

internal data class WooTooltipLayoutTokens(
    val windowMargin: Int,
    val anchorGap: Int,
    val maxBubbleWidth: Int,
    val minSideWidth: Int,
    val geometry: WooTooltipGeometryTokens,
)

internal data class WooTooltipLayoutResult(
    val offset: IntOffset,
    val side: WooTooltipPhysicalSide,
    val arrowEdge: WooTooltipPhysicalEdge,
    val arrowCenter: Float,
    val maxBubbleWidth: Int,
)

internal data class WooTooltipLayoutInput(
    val anchorBounds: IntRect,
    val windowSize: IntSize,
    val popupContentSize: IntSize,
    val layoutDirection: LayoutDirection,
    val preferredPlacement: WooTooltipPlacement?,
)

internal class WooTooltipPositionProvider(
    private val preferredPlacement: WooTooltipPlacement?,
    private val tokens: WooTooltipLayoutTokens,
    private val currentLayoutDirection: () -> LayoutDirection,
    private val onLayoutResult: (WooTooltipLayoutResult) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val result = calculateWooTooltipLayout(
            input = WooTooltipLayoutInput(
                anchorBounds = anchorBounds,
                windowSize = windowSize,
                popupContentSize = popupContentSize,
                layoutDirection = currentLayoutDirection(),
                preferredPlacement = preferredPlacement,
            ),
            tokens = tokens,
        )
        onLayoutResult(result)
        return result.offset
    }
}

internal fun calculateWooTooltipLayout(
    input: WooTooltipLayoutInput,
    tokens: WooTooltipLayoutTokens,
): WooTooltipLayoutResult {
    val room = input.availableRoom(tokens)
    val side = input.resolveSide(room, tokens)
    val offset = input.popupOffset(side, tokens)
    val arrowEdge = side.arrowEdge
    val desiredArrowCenter = input.desiredArrowCenter(arrowEdge, offset)
    val geometry = wooTooltipGeometry(
        edge = arrowEdge,
        desiredArrowCenter = desiredArrowCenter,
        size = Size(input.popupContentSize.width.toFloat(), input.popupContentSize.height.toFloat()),
        tokens = tokens.geometry,
    )

    return WooTooltipLayoutResult(
        offset = offset,
        side = side,
        arrowEdge = arrowEdge,
        arrowCenter = geometry?.arrowCenter ?: desiredArrowCenter,
        maxBubbleWidth = input.maxBubbleWidth(side, room, tokens),
    )
}

private fun WooTooltipLayoutInput.availableRoom(tokens: WooTooltipLayoutTokens) = WooTooltipAvailableRoom(
    above = anchorBounds.top - tokens.anchorGap - tokens.windowMargin,
    below = windowSize.height - tokens.windowMargin - anchorBounds.bottom - tokens.anchorGap,
    left = anchorBounds.left - tokens.anchorGap - tokens.windowMargin,
    right = windowSize.width - tokens.windowMargin - anchorBounds.right - tokens.anchorGap,
)

private fun WooTooltipLayoutInput.resolveSide(
    room: WooTooltipAvailableRoom,
    tokens: WooTooltipLayoutTokens,
): WooTooltipPhysicalSide {
    val preferredSide = preferredPlacement?.resolve(layoutDirection)
    return when (preferredSide) {
        null -> if (room.below >= room.above) WooTooltipPhysicalSide.Below else WooTooltipPhysicalSide.Above
        else -> resolvePreferredSide(preferredSide, room, popupContentSize, tokens.minSideWidth)
    }
}

private fun WooTooltipLayoutInput.maxBubbleWidth(
    side: WooTooltipPhysicalSide,
    room: WooTooltipAvailableRoom,
    tokens: WooTooltipLayoutTokens,
): Int = when (side) {
    WooTooltipPhysicalSide.Above,
    WooTooltipPhysicalSide.Below,
    -> min(tokens.maxBubbleWidth, max(0, windowSize.width - 2 * tokens.windowMargin))

    WooTooltipPhysicalSide.Left,
    WooTooltipPhysicalSide.Right,
    -> min(tokens.maxBubbleWidth, max(tokens.minSideWidth, room.forSide(side)))
}

private fun WooTooltipLayoutInput.popupOffset(
    side: WooTooltipPhysicalSide,
    tokens: WooTooltipLayoutTokens,
): IntOffset {
    val rawOffset = rawPopupOffset(side, tokens)
    return IntOffset(
        x = rawOffset.x.coerceToWindow(
            low = tokens.windowMargin,
            high = windowSize.width - tokens.windowMargin - popupContentSize.width,
        ),
        y = rawOffset.y.coerceToWindow(
            low = tokens.windowMargin,
            high = windowSize.height - tokens.windowMargin - popupContentSize.height,
        ),
    )
}

private fun WooTooltipLayoutInput.rawPopupOffset(
    side: WooTooltipPhysicalSide,
    tokens: WooTooltipLayoutTokens,
): IntOffset {
    val x = when (side) {
        WooTooltipPhysicalSide.Above,
        WooTooltipPhysicalSide.Below,
        -> anchorBounds.center.x - popupContentSize.width / 2

        WooTooltipPhysicalSide.Left -> anchorBounds.left - tokens.anchorGap - popupContentSize.width
        WooTooltipPhysicalSide.Right -> anchorBounds.right + tokens.anchorGap
    }
    val y = when (side) {
        WooTooltipPhysicalSide.Above -> anchorBounds.top - tokens.anchorGap - popupContentSize.height
        WooTooltipPhysicalSide.Below -> anchorBounds.bottom + tokens.anchorGap
        WooTooltipPhysicalSide.Left,
        WooTooltipPhysicalSide.Right,
        -> anchorBounds.center.y - popupContentSize.height / 2
    }
    return IntOffset(x, y)
}

private fun WooTooltipLayoutInput.desiredArrowCenter(
    arrowEdge: WooTooltipPhysicalEdge,
    popupOffset: IntOffset,
): Float = when (arrowEdge) {
    WooTooltipPhysicalEdge.Top,
    WooTooltipPhysicalEdge.Bottom,
    -> anchorBounds.center.x - popupOffset.x.toFloat()

    WooTooltipPhysicalEdge.Left,
    WooTooltipPhysicalEdge.Right,
    -> anchorBounds.center.y - popupOffset.y.toFloat()
}

internal fun isWooTooltipAnchorVisible(
    anchorBounds: androidx.compose.ui.geometry.Rect,
    windowSize: IntSize,
): Boolean = anchorBounds.width > 0f &&
    anchorBounds.height > 0f &&
    windowSize.width > 0 &&
    windowSize.height > 0 &&
    anchorBounds.right > 0f &&
    anchorBounds.left < windowSize.width &&
    anchorBounds.bottom > 0f &&
    anchorBounds.top < windowSize.height

private fun resolvePreferredSide(
    preferred: WooTooltipPhysicalSide,
    room: WooTooltipAvailableRoom,
    popupSize: IntSize,
    minSideWidth: Int,
): WooTooltipPhysicalSide {
    if (room.canFit(preferred, popupSize, minSideWidth)) return preferred
    return preferred.opposite
}

internal fun WooTooltipPlacement.resolve(layoutDirection: LayoutDirection): WooTooltipPhysicalSide = when (this) {
    WooTooltipPlacement.Above -> WooTooltipPhysicalSide.Above
    WooTooltipPlacement.Below -> WooTooltipPhysicalSide.Below
    WooTooltipPlacement.Start -> {
        if (layoutDirection == LayoutDirection.Ltr) WooTooltipPhysicalSide.Left else WooTooltipPhysicalSide.Right
    }

    WooTooltipPlacement.End -> {
        if (layoutDirection == LayoutDirection.Ltr) WooTooltipPhysicalSide.Right else WooTooltipPhysicalSide.Left
    }
}

private data class WooTooltipAvailableRoom(
    val above: Int,
    val below: Int,
    val left: Int,
    val right: Int,
) {
    fun forSide(side: WooTooltipPhysicalSide): Int = when (side) {
        WooTooltipPhysicalSide.Above -> above
        WooTooltipPhysicalSide.Below -> below
        WooTooltipPhysicalSide.Left -> left
        WooTooltipPhysicalSide.Right -> right
    }

    fun canFit(side: WooTooltipPhysicalSide, popupSize: IntSize, minSideWidth: Int): Boolean = when (side) {
        WooTooltipPhysicalSide.Above,
        WooTooltipPhysicalSide.Below,
        -> forSide(side) >= popupSize.height

        WooTooltipPhysicalSide.Left,
        WooTooltipPhysicalSide.Right,
        -> forSide(side) >= minSideWidth
    }
}

private val WooTooltipPhysicalSide.opposite: WooTooltipPhysicalSide
    get() = when (this) {
        WooTooltipPhysicalSide.Above -> WooTooltipPhysicalSide.Below
        WooTooltipPhysicalSide.Below -> WooTooltipPhysicalSide.Above
        WooTooltipPhysicalSide.Left -> WooTooltipPhysicalSide.Right
        WooTooltipPhysicalSide.Right -> WooTooltipPhysicalSide.Left
    }

private fun Int.coerceToWindow(low: Int, high: Int): Int = coerceIn(low, max(low, high))

internal enum class WooTooltipPhysicalSide {
    Above,
    Below,
    Left,
    Right,
}
