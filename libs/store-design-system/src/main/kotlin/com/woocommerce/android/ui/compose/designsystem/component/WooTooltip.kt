package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
fun WooTooltip(
    title: String,
    arrowPosition: WooTooltipArrowPosition,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Surface(
        modifier = modifier,
        color = WooTheme.colors.surface.inverted,
        contentColor = WooTheme.colors.surface.onInverted,
        shape = WooTooltipShape(
            arrowPosition = arrowPosition,
            cornerRadius = WooTheme.radius.large,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                arrowPosition.contentPadding(bodyPadding = WooTheme.padding.padding6)
            ),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        ) {
            Text(
                text = title,
                style = WooTheme.text.bodyMedium.emphasized,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = WooTheme.text.bodyMedium.regular,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WooTooltipPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooTooltipDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Preview(name = "Large font", fontScale = 1.5f, showBackground = true)
@Composable
private fun WooTooltipLargeFontPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooTooltip(
                title = "Tooltip title",
                supportingText = "Supporting information wraps and expands without clipping.",
                arrowPosition = WooTooltipArrowPosition.TopCenter,
                modifier = Modifier
                    .padding(WooTheme.padding.padding5)
                    .width(TOOLTIP_PREVIEW_WIDTH),
            )
        }
    }
}

@Preview(name = "RTL", locale = "ar", showBackground = true)
@Composable
private fun WooTooltipRtlPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooTooltipDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooTooltipDemo(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
    ) {
        WooTooltipArrowPosition.entries.forEach { arrowPosition ->
            Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
                Text(
                    text = arrowPosition.name,
                    color = WooTheme.colors.background.onSection,
                    style = WooTheme.text.bodySmall.regular,
                )
                WooTooltip(
                    title = "Title",
                    supportingText = "Supporting line text lorem ipsum dolor sit amet, consectetur",
                    arrowPosition = arrowPosition,
                    modifier = Modifier.width(TOOLTIP_PREVIEW_WIDTH),
                )
            }
        }
    }
}

private fun WooTooltipArrowPosition.contentPadding(bodyPadding: Dp): PaddingValues {
    val bodyAndArrowPadding = bodyPadding + ARROW_DEPTH

    return when (this) {
        WooTooltipArrowPosition.TopStart,
        WooTooltipArrowPosition.TopCenter,
        WooTooltipArrowPosition.TopEnd,
        -> PaddingValues(
            start = bodyPadding,
            top = bodyAndArrowPadding,
            end = bodyPadding,
            bottom = bodyPadding,
        )

        WooTooltipArrowPosition.BottomStart,
        WooTooltipArrowPosition.BottomCenter,
        WooTooltipArrowPosition.BottomEnd,
        -> PaddingValues(
            start = bodyPadding,
            top = bodyPadding,
            end = bodyPadding,
            bottom = bodyAndArrowPadding,
        )

        WooTooltipArrowPosition.StartTop,
        WooTooltipArrowPosition.StartCenter,
        WooTooltipArrowPosition.StartBottom,
        -> PaddingValues(
            start = bodyAndArrowPadding,
            top = bodyPadding,
            end = bodyPadding,
            bottom = bodyPadding,
        )

        WooTooltipArrowPosition.EndTop,
        WooTooltipArrowPosition.EndCenter,
        WooTooltipArrowPosition.EndBottom,
        -> PaddingValues(
            start = bodyPadding,
            top = bodyPadding,
            end = bodyAndArrowPadding,
            bottom = bodyPadding,
        )
    }
}

internal data class WooTooltipShape(
    private val arrowPosition: WooTooltipArrowPosition,
    private val cornerRadius: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val geometry = wooTooltipGeometry(
            arrowPosition = arrowPosition,
            size = size,
            layoutDirection = layoutDirection,
            density = density,
            cornerRadius = cornerRadius,
        ) ?: return Outline.Rectangle(EMPTY_RECT)

        return Outline.Generic(
            path = tooltipOutlinePath(geometry)
        )
    }
}

internal fun wooTooltipGeometry(
    arrowPosition: WooTooltipArrowPosition,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
    cornerRadius: Dp,
): WooTooltipGeometry? {
    if (!size.width.isFinite() || !size.height.isFinite() || size.width <= 0f || size.height <= 0f) {
        return null
    }

    val resolvedPosition = arrowPosition.resolve(layoutDirection)
    val requestedRadius = with(density) { cornerRadius.toPx() }
    val requestedArrowHalfBase = with(density) { ARROW_BASE.toPx() } / 2f
    val requestedArrowDepth = with(density) { ARROW_DEPTH.toPx() }
    val requestedEdgeOffset = with(density) { ARROW_EDGE_OFFSET.toPx() }
    val requestedValues = listOf(
        requestedRadius,
        requestedArrowHalfBase,
        requestedArrowDepth,
        requestedEdgeOffset,
    )
    if (requestedValues.any { !it.isFinite() || it <= 0f }) return null

    val edgeLength = when (resolvedPosition.edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> size.width

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> size.height
    }
    val containerLength = when (resolvedPosition.edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> size.height

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> size.width
    }
    val minimumEdgeLength = maxOf(
        2f * (requestedRadius + requestedArrowHalfBase),
        2f * requestedEdgeOffset,
    )
    val minimumContainerLength = requestedArrowDepth + 2f * requestedRadius
    val scale = minOf(
        1f,
        edgeLength / minimumEdgeLength,
        containerLength / minimumContainerLength,
    )
    if (!scale.isFinite() || scale <= 0f) return null

    val scaledRadius = requestedRadius * scale
    val scaledArrowHalfBase = requestedArrowHalfBase * scale
    val arrowDepth = requestedArrowDepth * scale
    val edgeOffset = requestedEdgeOffset * scale
    val bodyBounds = resolvedPosition.bodyBounds(size, arrowDepth)
    val radius = minOf(
        scaledRadius,
        bodyBounds.width / 2f,
        bodyBounds.height / 2f,
    )
    val arrowHalfBase = minOf(
        scaledArrowHalfBase,
        edgeLength / 2f - radius,
    )
    if (radius <= 0f || arrowHalfBase <= 0f || bodyBounds.width <= 0f || bodyBounds.height <= 0f) {
        return null
    }
    val minimumCenter = radius + arrowHalfBase
    val maximumCenter = edgeLength - minimumCenter
    val arrowCenter = alignedArrowCenter(
        edgeLength = edgeLength,
        alignment = resolvedPosition.alignment,
        edgeOffset = edgeOffset,
        minimumCenter = minimumCenter,
        maximumCenter = maximumCenter,
    )
    val (arrowTip, arrowBaseStart, arrowBaseEnd) = resolvedPosition.arrowPoints(
        size = size,
        bodyBounds = bodyBounds,
        radius = radius,
        arrowCenter = arrowCenter,
        arrowHalfBase = arrowHalfBase,
    )

    return WooTooltipGeometry(
        edge = resolvedPosition.edge,
        bodyBounds = bodyBounds,
        cornerRadius = radius,
        arrowDepth = arrowDepth,
        arrowTip = arrowTip,
        arrowHalfBase = arrowHalfBase,
        arrowBaseStart = arrowBaseStart,
        arrowBaseEnd = arrowBaseEnd,
        scale = scale,
    )
}

private fun alignedArrowCenter(
    edgeLength: Float,
    alignment: WooTooltipPhysicalAlignment,
    edgeOffset: Float,
    minimumCenter: Float,
    maximumCenter: Float,
): Float {
    val desiredCenter = when (alignment) {
        WooTooltipPhysicalAlignment.Start -> edgeOffset
        WooTooltipPhysicalAlignment.Center -> edgeLength / 2f
        WooTooltipPhysicalAlignment.End -> edgeLength - edgeOffset
    }
    return desiredCenter.coerceIn(minimumCenter, maximumCenter.coerceAtLeast(minimumCenter))
}

private fun ResolvedWooTooltipArrowPosition.bodyBounds(
    size: Size,
    arrowDepth: Float,
): Rect = when (edge) {
    WooTooltipPhysicalEdge.Top -> Rect(0f, arrowDepth, size.width, size.height)
    WooTooltipPhysicalEdge.Bottom -> Rect(0f, 0f, size.width, size.height - arrowDepth)
    WooTooltipPhysicalEdge.Left -> Rect(arrowDepth, 0f, size.width, size.height)
    WooTooltipPhysicalEdge.Right -> Rect(0f, 0f, size.width - arrowDepth, size.height)
}

private fun ResolvedWooTooltipArrowPosition.arrowPoints(
    size: Size,
    bodyBounds: Rect,
    radius: Float,
    arrowCenter: Float,
    arrowHalfBase: Float,
): Triple<Offset, Offset, Offset> {
    val edgeLength = when (edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> size.width

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> size.height
    }
    val baseStart = (arrowCenter - arrowHalfBase).coerceAtLeast(radius)
    val baseEnd = (arrowCenter + arrowHalfBase).coerceAtMost(edgeLength - radius)

    return when (edge) {
        WooTooltipPhysicalEdge.Top -> Triple(
            Offset(arrowCenter, 0f),
            Offset(baseStart, bodyBounds.top),
            Offset(baseEnd, bodyBounds.top),
        )

        WooTooltipPhysicalEdge.Bottom -> Triple(
            Offset(arrowCenter, size.height),
            Offset(baseStart, bodyBounds.bottom),
            Offset(baseEnd, bodyBounds.bottom),
        )

        WooTooltipPhysicalEdge.Left -> Triple(
            Offset(0f, arrowCenter),
            Offset(bodyBounds.left, baseStart),
            Offset(bodyBounds.left, baseEnd),
        )

        WooTooltipPhysicalEdge.Right -> Triple(
            Offset(size.width, arrowCenter),
            Offset(bodyBounds.right, baseStart),
            Offset(bodyBounds.right, baseEnd),
        )
    }
}

private fun tooltipOutlinePath(geometry: WooTooltipGeometry): Path {
    val bodyPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = geometry.bodyBounds,
                cornerRadius = CornerRadius(geometry.cornerRadius),
            )
        )
    }
    val arrowPath = Path().apply {
        moveTo(geometry.arrowBaseStart.x, geometry.arrowBaseStart.y)
        lineTo(geometry.arrowTip.x, geometry.arrowTip.y)
        lineTo(geometry.arrowBaseEnd.x, geometry.arrowBaseEnd.y)
        close()
    }

    return Path.combine(PathOperation.Union, bodyPath, arrowPath)
}

internal data class WooTooltipGeometry(
    val edge: WooTooltipPhysicalEdge,
    val bodyBounds: Rect,
    val cornerRadius: Float,
    val arrowDepth: Float,
    val arrowTip: Offset,
    val arrowHalfBase: Float,
    val arrowBaseStart: Offset,
    val arrowBaseEnd: Offset,
    val scale: Float,
)

internal fun WooTooltipArrowPosition.resolve(
    layoutDirection: LayoutDirection,
): ResolvedWooTooltipArrowPosition {
    val edge = when (this) {
        WooTooltipArrowPosition.TopStart,
        WooTooltipArrowPosition.TopCenter,
        WooTooltipArrowPosition.TopEnd,
        -> WooTooltipPhysicalEdge.Top

        WooTooltipArrowPosition.BottomStart,
        WooTooltipArrowPosition.BottomCenter,
        WooTooltipArrowPosition.BottomEnd,
        -> WooTooltipPhysicalEdge.Bottom

        WooTooltipArrowPosition.StartTop,
        WooTooltipArrowPosition.StartCenter,
        WooTooltipArrowPosition.StartBottom,
        -> if (layoutDirection == LayoutDirection.Ltr) WooTooltipPhysicalEdge.Left else WooTooltipPhysicalEdge.Right

        WooTooltipArrowPosition.EndTop,
        WooTooltipArrowPosition.EndCenter,
        WooTooltipArrowPosition.EndBottom,
        -> if (layoutDirection == LayoutDirection.Ltr) WooTooltipPhysicalEdge.Right else WooTooltipPhysicalEdge.Left
    }
    val alignment = when (this) {
        WooTooltipArrowPosition.TopStart,
        WooTooltipArrowPosition.BottomStart,
        -> if (layoutDirection == LayoutDirection.Ltr) {
            WooTooltipPhysicalAlignment.Start
        } else {
            WooTooltipPhysicalAlignment.End
        }

        WooTooltipArrowPosition.TopEnd,
        WooTooltipArrowPosition.BottomEnd,
        -> if (layoutDirection == LayoutDirection.Ltr) {
            WooTooltipPhysicalAlignment.End
        } else {
            WooTooltipPhysicalAlignment.Start
        }

        WooTooltipArrowPosition.TopCenter,
        WooTooltipArrowPosition.BottomCenter,
        WooTooltipArrowPosition.StartCenter,
        WooTooltipArrowPosition.EndCenter,
        -> WooTooltipPhysicalAlignment.Center

        WooTooltipArrowPosition.StartTop,
        WooTooltipArrowPosition.EndTop,
        -> WooTooltipPhysicalAlignment.Start

        WooTooltipArrowPosition.StartBottom,
        WooTooltipArrowPosition.EndBottom,
        -> WooTooltipPhysicalAlignment.End
    }

    return ResolvedWooTooltipArrowPosition(edge, alignment)
}

internal data class ResolvedWooTooltipArrowPosition(
    val edge: WooTooltipPhysicalEdge,
    val alignment: WooTooltipPhysicalAlignment,
)

internal enum class WooTooltipPhysicalEdge {
    Top,
    Bottom,
    Left,
    Right,
}

internal enum class WooTooltipPhysicalAlignment {
    Start,
    Center,
    End,
}

private val ARROW_DEPTH = 10.dp
private val ARROW_BASE = 22.dp
private val ARROW_EDGE_OFFSET = 31.dp
private val TOOLTIP_PREVIEW_WIDTH = 200.dp
private val EMPTY_RECT = Rect(0f, 0f, 0f, 0f)
