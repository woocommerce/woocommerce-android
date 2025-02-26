package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

/**
 * We implemented our custom card as the default Material card
 * uses source of light being at the top left corner, which is not
 * the case in our design.
 */
@Composable
fun WooPosCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    elevation: WooPosElevation = WooPosElevation.Medium,
    shadowType: ShadowType = ShadowType.Normal,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
    ) {
        Box(
            modifier = modifier
                .surface(
                    shape = shape,
                    backgroundColor = backgroundColor,
                    border = border,
                    elevation = elevation.value,
                    shadowType = shadowType
                )
                .semantics(mergeDescendants = false) {
                    isTraversalGroup = true
                }
                .pointerInput(Unit) {},
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}

@Composable
private fun Modifier.surface(
    shape: Shape,
    backgroundColor: Color,
    border: BorderStroke?,
    elevation: Dp,
    shadowType: ShadowType
): Modifier {
    return this
        .drawShadow(
            color = Color.Black,
            borderRadius = shape.toCornerRadius(LocalDensity.current),
            shadowRadius = elevation * shadowType.shadowRadiusCoefficient,
            alpha = shadowType.alpha,
            offsetX = 0.dp,
            offsetY = elevation * shadowType.offsetYCoefficient
        )
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .background(color = backgroundColor, shape = shape)
        .clip(shape)
}

@Composable
fun Shape.toCornerRadius(density: Density): Dp {
    return if (this is CornerBasedShape) {
        with(density) {
            topStart.toPx(Size.Unspecified, this).toDp()
        }
    } else {
        0.dp
    }
}

@Suppress("MagicNumber")
sealed class ShadowType {
    abstract val alpha: Float
    abstract val shadowRadiusCoefficient: Float
    abstract val offsetYCoefficient: Float

    data object Soft : ShadowType() {
        override val alpha = 0.1f
        override val shadowRadiusCoefficient = 1.4F
        override val offsetYCoefficient = 0.7f
    }

    data object Normal : ShadowType() {
        override val alpha = 0.24f
        override val shadowRadiusCoefficient = 1F
        override val offsetYCoefficient = 0.5f
    }
}

@Suppress("LongParameterList")
private fun Modifier.drawShadow(
    color: Color,
    alpha: Float,
    borderRadius: Dp,
    shadowRadius: Dp,
    offsetY: Dp,
    offsetX: Dp,
) = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val paint = Paint()
    val frameworkPaint = paint.asFrameworkPaint()
    frameworkPaint.color = Color.Transparent.toArgb()
    frameworkPaint.setShadowLayer(
        shadowRadius.toPx(),
        offsetX.toPx(),
        offsetY.toPx(),
        shadowColor
    )
    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            borderRadius.toPx(),
            borderRadius.toPx(),
            paint
        )
    }
}

@WooPosPreview
@Composable
fun WooPosCardPreviewSmall() {
    Preview(elevation = WooPosElevation.Medium)
}

@WooPosPreview
@Composable
fun WooPosCardPreviewLarge() {
    Preview(elevation = WooPosElevation.Large)
}

@Composable
private fun Preview(elevation: WooPosElevation) {
    WooPosTheme {
        WooPosCard(
            modifier = Modifier.padding(WooPosSpacing.Medium.value),
            shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
            elevation = elevation,
        ) {
            WooPosText(
                modifier = Modifier
                    .padding(WooPosSpacing.XLarge.value)
                    .fillMaxWidth(),
                text = "WooPosCard",
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
