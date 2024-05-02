package com.woocommerce.android.ui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.windowSizeClass
import com.woocommerce.android.util.TabletLayoutSetupHelper

fun Modifier.drawShadow(
    color: Color,
    backgroundColor: Color,
    alpha: Float = 0.5f,
    borderRadius: Dp = 0.dp,
    shadowRadius: Dp = 20.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    this.drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = backgroundColor.toArgb()
        frameworkPaint.setShadowLayer(
            shadowRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )
        it.drawRoundRect(
            0f,
            0f,
            this.size.width,
            this.size.height,
            borderRadius.toPx(),
            borderRadius.toPx(),
            paint
        )
    }
}

@Stable
fun Modifier.autoMirror(): Modifier = composed {
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        this.scale(scaleX = -1f, scaleY = 1f)
    } else {
        this
    }
}

@Composable
fun Modifier.paddingBasedOnWindowSize(): Modifier {
    val context = LocalContext.current
    val windowSizeClass = context.windowSizeClass
    val screenWidthDp = context.resources.configuration.screenWidthDp.dp

    val padding = when (windowSizeClass) {
        WindowSizeClass.Compact -> 0.dp
        WindowSizeClass.Medium -> screenWidthDp * TabletLayoutSetupHelper.MARGINS_FOR_SMALL_TABLET_PORTRAIT
        WindowSizeClass.ExpandedAndBigger -> screenWidthDp * TabletLayoutSetupHelper.MARGINS_FOR_TABLET
    }
    return this.padding(horizontal = padding)
}
