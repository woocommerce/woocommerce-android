package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding

@Composable
fun WooPosDialogWrapper(
    dialogBackgroundContentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val animVisibleState = remember { MutableTransitionState(false) }
        .apply { targetState = true }
    AnimatedVisibility(
        visibleState = animVisibleState,
        enter = fadeIn(initialAlpha = 0.3f),
        exit = fadeOut(targetAlpha = 0.0f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                .clickable(
                    onClick = { animVisibleState.targetState = false },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .semantics {
                    contentDescription = dialogBackgroundContentDescription
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visibleState = animVisibleState,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    initialOffsetY = { it / 8 },
                    animationSpec = tween(300)
                ),
                exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                    targetOffsetY = { it / 8 },
                    animationSpec = tween(300)
                ),
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 150.dp.toAdaptivePadding())
                ) {
                    content()
                }
            }
        }
    }
}
