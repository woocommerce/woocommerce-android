package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosCard

@Composable
fun WooPosDialogWrapper(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    dialogBackgroundContentDescription: String,
    onDismissRequest: () -> Unit,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        WooPosBackgroundOverlay(
            modifier = Modifier
                .semantics {
                    contentDescription = dialogBackgroundContentDescription
                },
            isVisible = isVisible,
            onClick = onDismissRequest
        )
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                initialOffsetY = { it / 8 },
                animationSpec = tween(300)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                targetOffsetY = { it / 8 },
                animationSpec = tween(300)
            ),
        ) {
            WooPosCard(
                shape = RoundedCornerShape(24.dp),
                elevation = 8.dp,
                modifier = modifier
            ) {
                content()
            }
        }
    }
}
