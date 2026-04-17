package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.woocommerce.android.R
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

@Composable
fun WooPosUpdateProgressIndicator(
    progress: Float,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    val progressColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.secondary
    val iconColor = MaterialTheme.colorScheme.onPrimary

    val animatedProgress = remember { Animatable(progress) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )
    }

    val currentProgress = animatedProgress.value

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            val size = size.minDimension
            val borderInset = size * 0.04f

            drawCircle(
                color = trackColor,
                radius = size / 2,
            )

            drawIntoCanvas { canvas ->
                val clipHeight = (1 - currentProgress) * size
                canvas.save()
                canvas.clipRect(
                    left = 0f,
                    top = clipHeight,
                    right = size,
                    bottom = size
                )
                drawCircle(
                    color = progressColor,
                    radius = (size - borderInset) / 2,
                )
                canvas.restore()
            }
        }

        Icon(
            painter = painterResource(
                id = if (isComplete) R.drawable.ic_check_24dp else R.drawable.ic_arrow_up_24dp
            ),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.fillMaxSize(0.7f)
        )
    }
}

@Composable
@WooPosPreview
fun PreviewWooPosUpdateProgressIndicator() {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            WooPosUpdateProgressIndicator(
                progress = 0.65f,
                isComplete = false,
                modifier = Modifier.size(WooPosComponentSize.Medium.value)
            )
        }
    }
}

@Composable
@WooPosPreview
fun PreviewWooPosUpdateProgressIndicatorComplete() {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            WooPosUpdateProgressIndicator(
                progress = 1.0f,
                isComplete = true,
                modifier = Modifier.size(WooPosComponentSize.Medium.value)
            )
        }
    }
}
