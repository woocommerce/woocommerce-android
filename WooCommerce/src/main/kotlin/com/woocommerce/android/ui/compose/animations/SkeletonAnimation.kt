package com.woocommerce.android.ui.compose.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SKELETON_BASE_ALPHA = 0.12f
private const val SKELETON_HIGHLIGHT_ALPHA = 0.20f

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

@Composable
private fun skeletonAnimationBrush(): Brush {
    val transition = rememberInfiniteTransition(
        label = "shimmer_transition"
    )
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "shimmer_animation"
    )

    val skeletonColor = MaterialTheme.colorScheme.onSurface
    val shimmerColorShades = listOf(
        skeletonColor.copy(alpha = SKELETON_BASE_ALPHA),
        skeletonColor.copy(alpha = SKELETON_HIGHLIGHT_ALPHA),
        skeletonColor.copy(alpha = SKELETON_BASE_ALPHA)
    )

    return Brush.linearGradient(
        colors = shimmerColorShades,
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Preview
@Composable
fun SkeletonViewPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        SkeletonView(
            modifier = Modifier.align(Alignment.Center),
            width = 200.dp,
            height = 100.dp
        )
    }
}
