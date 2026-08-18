@file:OptIn(ExperimentalMaterial3Api::class)

package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import kotlinx.coroutines.launch

/**
 * Hosts an always-composed [content] anchor and presents a Woo tooltip around it.
 *
 * Long press, hover, and keyboard focus use Material's standard tooltip triggers. Call [WooTooltipState.show] for
 * caller-controlled presentation. [modifier] applies to the anchor interaction wrapper, not the popup surface.
 * [preferredPlacement] requests a logical side and always flips to its opposite when it cannot be used; null
 * chooses the roomier vertical side.
 */
@Composable
fun WooTooltipBox(
    state: WooTooltipState,
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    preferredPlacement: WooTooltipPlacement? = null,
    content: @Composable () -> Unit,
) = WooTooltipBoxImpl(
    state = state,
    title = title,
    modifier = modifier,
    supportingText = supportingText,
    preferredPlacement = preferredPlacement,
    content = content,
)

@Composable
internal fun WooTooltipBoxImpl(
    state: WooTooltipState,
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    preferredPlacement: WooTooltipPlacement? = null,
    onLayoutResult: (WooTooltipLayoutResult) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val windowInfo = LocalWindowInfo.current
    val cornerRadius = WooTheme.radius.large
    val scope = rememberCoroutineScope()
    var anchorIsVisible by remember { mutableStateOf(false) }
    val currentLayoutDirection = rememberUpdatedState(layoutDirection)
    val currentOnLayoutResult = rememberUpdatedState(onLayoutResult)
    val geometryTokens = remember(density, cornerRadius) {
        wooTooltipGeometryTokens(density, cornerRadius)
    }
    val layoutTokens = remember(density, geometryTokens) {
        WooTooltipLayoutTokens(
            windowMargin = with(density) { WINDOW_MARGIN.roundToPx() },
            anchorGap = with(density) { ANCHOR_GAP.roundToPx() },
            maxBubbleWidth = with(density) { MAX_TOOLTIP_WIDTH.roundToPx() },
            minSideWidth = with(density) { MIN_SIDE_WIDTH.roundToPx() },
            geometry = geometryTokens,
        )
    }
    var layoutResult by remember {
        mutableStateOf(
            WooTooltipLayoutResult(
                offset = androidx.compose.ui.unit.IntOffset.Zero,
                side = WooTooltipPhysicalSide.Below,
                arrowEdge = WooTooltipPhysicalEdge.Top,
                arrowCenter = Float.NaN,
                maxBubbleWidth = layoutTokens.maxBubbleWidth,
            )
        )
    }
    val positionProvider = remember(preferredPlacement, layoutTokens) {
        WooTooltipPositionProvider(
            preferredPlacement = preferredPlacement,
            tokens = layoutTokens,
            currentLayoutDirection = { currentLayoutDirection.value },
            onLayoutResult = { result ->
                if (layoutResult != result) layoutResult = result
                currentOnLayoutResult.value(result)
            },
        )
    }
    val popupMaxWidth = with(density) { layoutResult.maxBubbleWidth.coerceAtLeast(1).toDp() }

    LaunchedEffect(state) {
        state.onAnchorVisibilityChanged(anchorIsVisible)
        if (anchorIsVisible) state.resumeOffscreenRequest()
    }

    TooltipBox(
        positionProvider = positionProvider,
        tooltip = {
            WooTooltipSurface(
                title = title,
                supportingText = supportingText,
                arrowEdge = layoutResult.arrowEdge,
                arrowCenter = layoutResult.arrowCenter,
                cornerRadius = cornerRadius,
                modifier = Modifier
                    .widthIn(max = popupMaxWidth)
                    .pointerInput(state) {
                        detectTapGestures { state.dismiss() }
                    },
            )
        },
        state = state.hostState,
        modifier = modifier.onGloballyPositioned { coordinates ->
            val isVisible = isWooTooltipAnchorVisible(
                anchorBounds = coordinates.boundsInWindow(),
                windowSize = windowInfo.containerSize,
            )
            if (anchorIsVisible != isVisible) {
                anchorIsVisible = isVisible
                state.onAnchorVisibilityChanged(isVisible)
                if (isVisible) scope.launch { state.resumeOffscreenRequest() }
            }
        },
        focusable = false,
        enableUserInput = true,
        hasAction = false,
        content = content,
    )
}

@Composable
internal fun WooTooltipSurface(
    title: String,
    supportingText: String?,
    arrowEdge: WooTooltipPhysicalEdge,
    arrowCenter: Float,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = WooTheme.colors.surface.inverted,
        contentColor = WooTheme.colors.surface.onInverted,
        shape = WooTooltipShape(
            arrowEdge = arrowEdge,
            desiredArrowCenter = arrowCenter,
            cornerRadius = cornerRadius,
        ),
    ) {
        Column(
            modifier = Modifier.tooltipContentPadding(arrowEdge, WooTheme.padding.padding6),
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

private fun Modifier.tooltipContentPadding(
    arrowEdge: WooTooltipPhysicalEdge,
    bodyPadding: Dp,
): Modifier {
    val bodyAndArrowPadding = bodyPadding + ARROW_DEPTH
    return when (arrowEdge) {
        WooTooltipPhysicalEdge.Top -> absolutePadding(
            left = bodyPadding,
            top = bodyAndArrowPadding,
            right = bodyPadding,
            bottom = bodyPadding,
        )

        WooTooltipPhysicalEdge.Bottom -> absolutePadding(
            left = bodyPadding,
            top = bodyPadding,
            right = bodyPadding,
            bottom = bodyAndArrowPadding,
        )

        WooTooltipPhysicalEdge.Left -> absolutePadding(
            left = bodyAndArrowPadding,
            top = bodyPadding,
            right = bodyPadding,
            bottom = bodyPadding,
        )

        WooTooltipPhysicalEdge.Right -> absolutePadding(
            left = bodyPadding,
            top = bodyPadding,
            right = bodyAndArrowPadding,
            bottom = bodyPadding,
        )
    }
}

internal data class WooTooltipShape(
    private val arrowEdge: WooTooltipPhysicalEdge,
    private val desiredArrowCenter: Float,
    private val cornerRadius: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val geometry = wooTooltipGeometry(
            edge = arrowEdge,
            desiredArrowCenter = desiredArrowCenter,
            size = size,
            tokens = wooTooltipGeometryTokens(density, cornerRadius),
        ) ?: return Outline.Rectangle(EMPTY_RECT)

        return Outline.Generic(tooltipOutlinePath(geometry))
    }
}

internal data class WooTooltipGeometryTokens(
    val cornerRadius: Float,
    val arrowHalfBase: Float,
    val arrowDepth: Float,
)

internal fun wooTooltipGeometryTokens(
    density: Density,
    cornerRadius: Dp,
): WooTooltipGeometryTokens = with(density) {
    WooTooltipGeometryTokens(
        cornerRadius = cornerRadius.toPx(),
        arrowHalfBase = ARROW_BASE.toPx() / 2f,
        arrowDepth = ARROW_DEPTH.toPx(),
    )
}

internal fun wooTooltipGeometry(
    edge: WooTooltipPhysicalEdge,
    desiredArrowCenter: Float,
    size: Size,
    tokens: WooTooltipGeometryTokens,
): WooTooltipGeometry? {
    val requestedValues = listOf(tokens.cornerRadius, tokens.arrowHalfBase, tokens.arrowDepth)
    if (!size.width.isFinite() || !size.height.isFinite() || size.width <= 0f || size.height <= 0f) return null
    if (requestedValues.any { !it.isFinite() || it <= 0f }) return null

    val edgeLength = edge.edgeLength(size)
    val containerLength = edge.containerLength(size)
    val scale = minOf(
        1f,
        edgeLength / (2f * (tokens.cornerRadius + tokens.arrowHalfBase)),
        containerLength / (tokens.arrowDepth + 2f * tokens.cornerRadius),
    )
    if (!scale.isFinite() || scale <= 0f) return null

    val arrowDepth = tokens.arrowDepth * scale
    val bodyBounds = edge.bodyBounds(size, arrowDepth)
    val cornerRadius = minOf(
        tokens.cornerRadius * scale,
        bodyBounds.width / 2f,
        bodyBounds.height / 2f,
    )
    val arrowHalfBase = minOf(
        tokens.arrowHalfBase * scale,
        edgeLength / 2f - cornerRadius,
    )
    if (cornerRadius <= 0f || arrowHalfBase <= 0f || bodyBounds.width <= 0f || bodyBounds.height <= 0f) {
        return null
    }
    val minimumCenter = cornerRadius + arrowHalfBase
    val maximumCenter = edgeLength - minimumCenter
    val requestedCenter = desiredArrowCenter.takeIf(Float::isFinite) ?: edgeLength / 2f
    val arrowCenter = requestedCenter.coerceIn(minimumCenter, maximumCenter.coerceAtLeast(minimumCenter))
    val (arrowTip, arrowBaseStart, arrowBaseEnd) = edge.arrowPoints(
        size = size,
        bodyBounds = bodyBounds,
        arrowCenter = arrowCenter,
        arrowHalfBase = arrowHalfBase,
    )

    return WooTooltipGeometry(
        edge = edge,
        bodyBounds = bodyBounds,
        cornerRadius = cornerRadius,
        arrowDepth = arrowDepth,
        arrowCenter = arrowCenter,
        arrowHalfBase = arrowHalfBase,
        arrowTip = arrowTip,
        arrowBaseStart = arrowBaseStart,
        arrowBaseEnd = arrowBaseEnd,
        scale = scale,
    )
}

private fun WooTooltipPhysicalEdge.edgeLength(size: Size): Float = when (this) {
    WooTooltipPhysicalEdge.Top,
    WooTooltipPhysicalEdge.Bottom,
    -> size.width

    WooTooltipPhysicalEdge.Left,
    WooTooltipPhysicalEdge.Right,
    -> size.height
}

private fun WooTooltipPhysicalEdge.containerLength(size: Size): Float = when (this) {
    WooTooltipPhysicalEdge.Top,
    WooTooltipPhysicalEdge.Bottom,
    -> size.height

    WooTooltipPhysicalEdge.Left,
    WooTooltipPhysicalEdge.Right,
    -> size.width
}

private fun WooTooltipPhysicalEdge.bodyBounds(size: Size, arrowDepth: Float): Rect = when (this) {
    WooTooltipPhysicalEdge.Top -> Rect(0f, arrowDepth, size.width, size.height)
    WooTooltipPhysicalEdge.Bottom -> Rect(0f, 0f, size.width, size.height - arrowDepth)
    WooTooltipPhysicalEdge.Left -> Rect(arrowDepth, 0f, size.width, size.height)
    WooTooltipPhysicalEdge.Right -> Rect(0f, 0f, size.width - arrowDepth, size.height)
}

private fun WooTooltipPhysicalEdge.arrowPoints(
    size: Size,
    bodyBounds: Rect,
    arrowCenter: Float,
    arrowHalfBase: Float,
): Triple<Offset, Offset, Offset> {
    val baseStart = arrowCenter - arrowHalfBase
    val baseEnd = arrowCenter + arrowHalfBase
    return when (this) {
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
    val arrowCenter: Float,
    val arrowHalfBase: Float,
    val arrowTip: Offset,
    val arrowBaseStart: Offset,
    val arrowBaseEnd: Offset,
    val scale: Float,
)

internal enum class WooTooltipPhysicalEdge {
    Top,
    Bottom,
    Left,
    Right,
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

@Preview(name = "Title-only side", showBackground = true)
@Composable
private fun WooTooltipTitleOnlySidePreview() {
    WooDesignSystemTheme {
        val state = rememberWooTooltipState()
        LaunchedEffect(state) { state.show() }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            WooTooltipBox(
                state = state,
                title = "Title",
                preferredPlacement = WooTooltipPlacement.End,
            ) {
                Text("Anchor")
            }
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
internal fun WooTooltipDemo(modifier: Modifier = Modifier) {
    val automaticState = rememberWooTooltipState()
    val preferredState = rememberWooTooltipState()
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
    ) {
        Text(
            text = "Long press, focus, or tap an anchor to show its tooltip.",
            color = WooTheme.colors.background.onSection,
            style = WooTheme.text.bodySmall.regular,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4)) {
            WooTooltipBox(
                state = automaticState,
                title = "Automatic placement",
                supportingText = "The roomier vertical side is selected.",
            ) {
                Button(onClick = { scope.launch { automaticState.show() } }) {
                    Text("Automatic")
                }
            }
            WooTooltipBox(
                state = preferredState,
                title = "Title only",
                preferredPlacement = WooTooltipPlacement.End,
            ) {
                Button(onClick = { scope.launch { preferredState.show() } }) {
                    Text("Preferred end")
                }
            }
        }
    }
}

internal val WooTooltipPhysicalSide.arrowEdge: WooTooltipPhysicalEdge
    get() = when (this) {
        WooTooltipPhysicalSide.Above -> WooTooltipPhysicalEdge.Bottom
        WooTooltipPhysicalSide.Below -> WooTooltipPhysicalEdge.Top
        WooTooltipPhysicalSide.Left -> WooTooltipPhysicalEdge.Right
        WooTooltipPhysicalSide.Right -> WooTooltipPhysicalEdge.Left
    }

private val WINDOW_MARGIN = 8.dp
private val ANCHOR_GAP = 4.dp
private val MAX_TOOLTIP_WIDTH = 200.dp
private val MIN_SIDE_WIDTH = 80.dp
private val ARROW_DEPTH = 10.dp
private val ARROW_BASE = 22.dp
private val EMPTY_RECT = Rect(0f, 0f, 0f, 0f)
