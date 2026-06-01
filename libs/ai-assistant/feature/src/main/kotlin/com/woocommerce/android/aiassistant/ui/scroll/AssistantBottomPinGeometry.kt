package com.woocommerce.android.aiassistant.ui.scroll

internal fun isRenderedTargetPinnedAboveBottomContent(
    distanceFromViewportEnd: Int,
    bottomContentPaddingPx: Int,
    bottomPinThresholdPx: Int,
): Boolean = distanceFromViewportEnd - bottomContentPaddingPx >= -bottomPinThresholdPx

internal fun scrollDeltaToRevealTargetAboveBottomContent(
    distanceFromViewportEnd: Int,
    bottomContentPaddingPx: Int,
): Float = (bottomContentPaddingPx - distanceFromViewportEnd)
    .coerceAtLeast(0)
    .toFloat()
