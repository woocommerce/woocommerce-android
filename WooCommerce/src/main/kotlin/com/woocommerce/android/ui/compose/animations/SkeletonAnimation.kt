package com.woocommerce.android.ui.compose.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import com.woocommerce.android.R

const val SKELETON_ANIMATION_ALPHA = 0.2F

@Composable
fun skeletonAnimationBrush(): Brush {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        )
    )

    val shimmerColorShades = listOf(
        colorResource(id = R.color.skeleton_color),
        colorResource(id = R.color.skeleton_color).copy(SKELETON_ANIMATION_ALPHA),
        colorResource(id = R.color.skeleton_color)
    )

    return Brush.linearGradient(
        colors = shimmerColorShades,
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun SkeletonView(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier
            .width(width)
            .height(height)
            .background(skeletonAnimationBrush())
    )
}

@Composable
fun SkeletonView(modifier: Modifier) {
    Spacer(
        modifier = modifier.background(skeletonAnimationBrush())
    )
}
