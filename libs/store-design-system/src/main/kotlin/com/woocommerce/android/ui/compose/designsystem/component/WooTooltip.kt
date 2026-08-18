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

private class WooTooltipShape(
    private val arrowPosition: WooTooltipArrowPosition,
    private val cornerRadius: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val resolvedPosition = arrowPosition.resolve(layoutDirection)
        val arrowDepth = with(density) { ARROW_DEPTH.toPx() }.coerceAtMost(
            when (resolvedPosition.edge) {
                WooTooltipPhysicalEdge.Top,
                WooTooltipPhysicalEdge.Bottom,
                -> size.height

                WooTooltipPhysicalEdge.Left,
                WooTooltipPhysicalEdge.Right,
                -> size.width
            }
        )
        val arrowHalfBase = with(density) { ARROW_BASE.toPx() } / 2f
        val bodyBounds = resolvedPosition.bodyBounds(size, arrowDepth)
        val radius = with(density) { cornerRadius.toPx() }
            .coerceAtMost(bodyBounds.width / 2f)
            .coerceAtMost(bodyBounds.height / 2f)
        val arrowTip = wooTooltipArrowTip(
            arrowPosition = arrowPosition,
            size = size,
            layoutDirection = layoutDirection,
            density = density,
            cornerRadius = cornerRadius,
        )

        return Outline.Generic(
            path = tooltipOutlinePath(
                bodyBounds = bodyBounds,
                cornerRadius = radius,
                arrowTip = arrowTip,
                arrowHalfBase = arrowHalfBase,
                arrowEdge = resolvedPosition.edge,
            )
        )
    }
}

internal fun wooTooltipArrowTip(
    arrowPosition: WooTooltipArrowPosition,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
    cornerRadius: Dp,
): Offset {
    val resolvedPosition = arrowPosition.resolve(layoutDirection)
    val edgeOffset = with(density) { ARROW_EDGE_OFFSET.toPx() }
    val arrowHalfBase = with(density) { ARROW_BASE.toPx() } / 2f
    val minimumInset = with(density) { cornerRadius.toPx() } + arrowHalfBase
    val axisLength = when (resolvedPosition.edge) {
        WooTooltipPhysicalEdge.Top,
        WooTooltipPhysicalEdge.Bottom,
        -> size.width

        WooTooltipPhysicalEdge.Left,
        WooTooltipPhysicalEdge.Right,
        -> size.height
    }
    val arrowCenter = arrowCenter(
        axisLength = axisLength,
        alignment = resolvedPosition.alignment,
        edgeOffset = edgeOffset,
        minimumInset = minimumInset,
    )

    return when (resolvedPosition.edge) {
        WooTooltipPhysicalEdge.Top -> Offset(arrowCenter, 0f)
        WooTooltipPhysicalEdge.Bottom -> Offset(arrowCenter, size.height)
        WooTooltipPhysicalEdge.Left -> Offset(0f, arrowCenter)
        WooTooltipPhysicalEdge.Right -> Offset(size.width, arrowCenter)
    }
}

private fun arrowCenter(
    axisLength: Float,
    alignment: WooTooltipPhysicalAlignment,
    edgeOffset: Float,
    minimumInset: Float,
): Float {
    val safeInset = minimumInset.coerceAtMost(axisLength / 2f)
    val desiredCenter = when (alignment) {
        WooTooltipPhysicalAlignment.Start -> edgeOffset
        WooTooltipPhysicalAlignment.Center -> axisLength / 2f
        WooTooltipPhysicalAlignment.End -> axisLength - edgeOffset
    }
    return desiredCenter.coerceIn(safeInset, axisLength - safeInset)
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

private fun tooltipOutlinePath(
    bodyBounds: Rect,
    cornerRadius: Float,
    arrowTip: Offset,
    arrowHalfBase: Float,
    arrowEdge: WooTooltipPhysicalEdge,
): Path {
    val bodyPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = bodyBounds,
                cornerRadius = CornerRadius(cornerRadius),
            )
        )
    }
    val arrowPath = Path().apply {
        when (arrowEdge) {
            WooTooltipPhysicalEdge.Top,
            WooTooltipPhysicalEdge.Bottom,
            -> {
                moveTo(arrowTip.x - arrowHalfBase, bodyBounds.verticalArrowEdge(arrowEdge))
                lineTo(arrowTip.x, arrowTip.y)
                lineTo(arrowTip.x + arrowHalfBase, bodyBounds.verticalArrowEdge(arrowEdge))
            }

            WooTooltipPhysicalEdge.Left,
            WooTooltipPhysicalEdge.Right,
            -> {
                moveTo(bodyBounds.horizontalArrowEdge(arrowEdge), arrowTip.y - arrowHalfBase)
                lineTo(arrowTip.x, arrowTip.y)
                lineTo(bodyBounds.horizontalArrowEdge(arrowEdge), arrowTip.y + arrowHalfBase)
            }
        }
        close()
    }

    return Path.combine(PathOperation.Union, bodyPath, arrowPath)
}

private fun Rect.verticalArrowEdge(edge: WooTooltipPhysicalEdge): Float = when (edge) {
    WooTooltipPhysicalEdge.Top -> top
    WooTooltipPhysicalEdge.Bottom -> bottom
    WooTooltipPhysicalEdge.Left,
    WooTooltipPhysicalEdge.Right,
    -> error("Only top and bottom edges have vertical arrow bounds.")
}

private fun Rect.horizontalArrowEdge(edge: WooTooltipPhysicalEdge): Float = when (edge) {
    WooTooltipPhysicalEdge.Left -> left
    WooTooltipPhysicalEdge.Right -> right
    WooTooltipPhysicalEdge.Top,
    WooTooltipPhysicalEdge.Bottom,
    -> error("Only left and right edges have horizontal arrow bounds.")
}

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
