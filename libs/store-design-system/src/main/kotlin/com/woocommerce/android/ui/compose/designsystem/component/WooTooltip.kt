@file:OptIn(ExperimentalMaterial3Api::class)

package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
 * chooses the roomier vertical side. When positioning observes that the anchor has left the viewport, the current
 * tooltip presentation is dismissed. Returning the anchor does not automatically show it again. [action] adds one
 * dismissing button whose callback is invoked only by that button.
 *
 * @param onDismissRequest called when the user clicks outside the tooltip. When null, Material dismisses the tooltip
 * automatically. When non-null, Material calls it instead and the callback controls whether the state is dismissed.
 */
@Composable
fun WooTooltipBox(
    state: WooTooltipState,
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    preferredPlacement: WooTooltipPlacement? = null,
    onDismissRequest: (() -> Unit)? = null,
    action: WooTooltipAction? = null,
    content: @Composable () -> Unit,
) = WooTooltipBoxImpl(
    state = state,
    title = title,
    modifier = modifier,
    supportingText = supportingText,
    preferredPlacement = preferredPlacement,
    onDismissRequest = onDismissRequest,
    action = action,
    content = content,
)

@Composable
internal fun WooTooltipBoxImpl(
    state: WooTooltipState,
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    preferredPlacement: WooTooltipPlacement? = null,
    onDismissRequest: (() -> Unit)? = null,
    action: WooTooltipAction? = null,
    onLayoutResult: (WooTooltipLayoutResult) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val windowInfo = LocalWindowInfo.current
    val cornerRadius = WooTheme.radius.large
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
                offset = IntOffset.Zero,
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

    TooltipBox(
        positionProvider = positionProvider,
        tooltip = {
            WooTooltipSurface(
                title = title,
                supportingText = supportingText,
                action = action,
                onActionClick = { tooltipAction ->
                    state.dismiss()
                    tooltipAction.onClick()
                },
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
        state = state.materialState,
        modifier = modifier.onGloballyPositioned { coordinates ->
            if (
                !isWooTooltipAnchorVisible(
                    anchorBounds = coordinates.boundsInWindow(),
                    windowSize = windowInfo.containerSize,
                )
            ) {
                state.dismiss()
            }
        },
        onDismissRequest = onDismissRequest,
        focusable = false,
        enableUserInput = true,
        hasAction = action != null,
        content = content,
    )
}

@Composable
internal fun WooTooltipSurface(
    title: String,
    supportingText: String?,
    action: WooTooltipAction? = null,
    onActionClick: (WooTooltipAction) -> Unit = { it.onClick() },
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
            if (action != null) {
                TextButton(
                    onClick = { onActionClick(action) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = WooTheme.colors.surface.onInverted,
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = action.label,
                        modifier = Modifier.weight(1f),
                        color = WooTheme.colors.surface.onInverted,
                        style = WooTheme.text.labelLarge.emphasized,
                        textAlign = TextAlign.Start,
                    )
                }
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

private data class WooTooltipScaledDimensions(
    val scale: Float,
    val arrowDepth: Float,
    val bodyBounds: Rect,
    val cornerRadius: Float,
    val arrowHalfBase: Float,
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
    val scaledDimensions = edge.scaledDimensions(size, tokens) ?: return null
    val edgeLength = edge.edgeLength(size)
    val minimumCenter = scaledDimensions.cornerRadius + scaledDimensions.arrowHalfBase
    val maximumCenter = edgeLength - minimumCenter
    val requestedCenter = desiredArrowCenter.takeIf(Float::isFinite) ?: edgeLength / 2f
    val arrowCenter = requestedCenter.coerceIn(minimumCenter, maximumCenter.coerceAtLeast(minimumCenter))
    val (arrowTip, arrowBaseStart, arrowBaseEnd) = edge.arrowPoints(
        size = size,
        bodyBounds = scaledDimensions.bodyBounds,
        arrowCenter = arrowCenter,
        arrowHalfBase = scaledDimensions.arrowHalfBase,
    )

    return WooTooltipGeometry(
        edge = edge,
        bodyBounds = scaledDimensions.bodyBounds,
        cornerRadius = scaledDimensions.cornerRadius,
        arrowDepth = scaledDimensions.arrowDepth,
        arrowCenter = arrowCenter,
        arrowHalfBase = scaledDimensions.arrowHalfBase,
        arrowTip = arrowTip,
        arrowBaseStart = arrowBaseStart,
        arrowBaseEnd = arrowBaseEnd,
        scale = scaledDimensions.scale,
    )
}

private fun WooTooltipPhysicalEdge.scaledDimensions(
    size: Size,
    tokens: WooTooltipGeometryTokens,
): WooTooltipScaledDimensions? {
    if (!size.hasValidTooltipDimensions || !tokens.hasValidTooltipDimensions) return null

    val edgeLength = edgeLength(size)
    val scale = minOf(
        1f,
        edgeLength / (2f * (tokens.cornerRadius + tokens.arrowHalfBase)),
        containerLength(size) / (tokens.arrowDepth + 2f * tokens.cornerRadius),
    )
    if (!scale.isFiniteAndPositive()) return null

    val arrowDepth = tokens.arrowDepth * scale
    val bodyBounds = bodyBounds(size, arrowDepth)
    val cornerRadius = minOf(
        tokens.cornerRadius * scale,
        bodyBounds.width / 2f,
        bodyBounds.height / 2f,
    )
    val arrowHalfBase = minOf(
        tokens.arrowHalfBase * scale,
        edgeLength / 2f - cornerRadius,
    )
    return WooTooltipScaledDimensions(
        scale = scale,
        arrowDepth = arrowDepth,
        bodyBounds = bodyBounds,
        cornerRadius = cornerRadius,
        arrowHalfBase = arrowHalfBase,
    ).takeUnless(WooTooltipScaledDimensions::isDegenerate)
}

private val Size.hasValidTooltipDimensions: Boolean
    get() = listOf(width, height).all(Float::isFiniteAndPositive)

private val WooTooltipGeometryTokens.hasValidTooltipDimensions: Boolean
    get() = listOf(cornerRadius, arrowHalfBase, arrowDepth).all(Float::isFiniteAndPositive)

private val WooTooltipScaledDimensions.isDegenerate: Boolean
    get() = listOf(cornerRadius, arrowHalfBase, bodyBounds.width, bodyBounds.height).any { it <= 0f }

private fun Float.isFiniteAndPositive(): Boolean = isFinite() && this > 0f

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

@PreviewLightDark
@Composable
private fun WooTooltipActionSurfacePreview() {
    WooTooltipSurfacePreview(
        title = "Customise your store",
        supportingText = "You can return to these settings at any time.",
        actionLabel = "Review advanced store settings",
    )
}

@Preview(name = "Action surface RTL", locale = "ar", widthDp = 240, showBackground = true)
@Composable
private fun WooTooltipActionSurfaceRtlPreview() {
    WooTooltipSurfacePreview(
        title = "خصّص متجرك",
        supportingText = "يمكنك العودة إلى هذه الإعدادات في أي وقت.",
        actionLabel = "مراجعة إعدادات المتجر المتقدمة",
    )
}

@Preview(name = "Action surface large font", fontScale = 2f, widthDp = 240, showBackground = true)
@Composable
private fun WooTooltipActionSurfaceLargeFontPreview() {
    WooTooltipSurfacePreview(
        title = "Customise your store",
        supportingText = "You can return to these settings at any time.",
        actionLabel = "Review advanced store settings",
    )
}

@Composable
private fun WooTooltipSurfacePreview(
    title: String,
    supportingText: String,
    actionLabel: String,
) {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooTooltipSurface(
                title = title,
                supportingText = supportingText,
                action = WooTooltipAction(label = actionLabel, onClick = {}),
                arrowEdge = WooTooltipPhysicalEdge.Top,
                arrowCenter = Float.NaN,
                cornerRadius = WooTheme.radius.large,
                modifier = Modifier
                    .padding(WooTheme.padding.padding5)
                    .widthIn(max = MAX_TOOLTIP_WIDTH),
            )
        }
    }
}

@Composable
internal fun WooTooltipDemo(modifier: Modifier = Modifier) {
    val automaticState = rememberWooTooltipState()
    val preferredState = rememberWooTooltipState()
    val actionState = rememberWooTooltipState()
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
        WooTooltipBox(
            state = actionState,
            title = "Actionable tooltip",
            supportingText = "The action dismisses this tooltip before its callback runs.",
            action = WooTooltipAction(label = "Got it", onClick = {}),
        ) {
            Button(onClick = { scope.launch { actionState.show() } }) {
                Text("With action")
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
