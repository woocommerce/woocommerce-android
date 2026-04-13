package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme

@Composable
fun WooPosCircularLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    WooPosLoadingIndicatorInternal(
        modifier = modifier,
        spinnerPrimaryColor = MaterialTheme.colorScheme.primary,
        spinnerSecondaryColor = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
fun WooPosButtonLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    WooPosLoadingIndicatorInternal(
        modifier = modifier,
        spinnerPrimaryColor = MaterialTheme.colorScheme.onPrimary,
        spinnerSecondaryColor = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun WooPosLoadingIndicatorInternal(
    modifier: Modifier = Modifier,
    spinnerPrimaryColor: Color,
    spinnerSecondaryColor: Color,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RotationTransition")
    val animatedRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
        ),
        label = "RotationAnimation"
    )

    Canvas(
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        val radius = size.width / 2

        drawCircle(
            color = spinnerSecondaryColor,
            radius = radius,
        )

        rotate(animatedRotation) {
            drawArc(
                color = spinnerPrimaryColor,
                startAngle = 0f,
                sweepAngle = 110f,
                useCenter = true,
                style = Fill,
            )
        }

        drawCircle(
            color = Color.White,
            radius = radius * 0.4f,
            blendMode = BlendMode.DstOut
        )
    }
}

@Composable
@WooPosPreview
fun PreviewWooPosCircularLoadingIndicator() {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            WooPosCircularLoadingIndicator(
                modifier = Modifier.size(WooPosComponentSize.XLarge.value)
            )
        }
    }
}

@Composable
@WooPosPreview
fun PreviewWooPosButtonLoadingIndicator() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            WooPosButtonLoadingIndicator(
                modifier = Modifier.size(WooPosIconSize.Medium.value)
            )
        }
    }
}
