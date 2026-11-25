package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

private const val ANIMATION_DURATION_MS = 300
private const val SCRIM_ALPHA = 0.4f

@Composable
fun WooPosBottomSheet(
    isVisible: Boolean,
    scrimContentDescription: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    BackHandler(enabled = isVisible) { onDismissRequest() }
    AnimatedVisibility(
        visible = isVisible,
        modifier = Modifier.fillMaxSize(),
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .animateEnterExit(
                        enter = fadeIn(animationSpec = tween(ANIMATION_DURATION_MS)),
                        exit = fadeOut(animationSpec = tween(ANIMATION_DURATION_MS)),
                    )
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
                    .semantics { contentDescription = scrimContentDescription }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )
            Surface(
                modifier = modifier
                    .animateEnterExit(
                        enter = slideInVertically(
                            animationSpec = tween(ANIMATION_DURATION_MS),
                            initialOffsetY = { it },
                        ),
                        exit = slideOutVertically(
                            animationSpec = tween(ANIMATION_DURATION_MS),
                            targetOffsetY = { it },
                        ),
                    )
                    .padding(horizontal = WooPosSpacing.Small.value)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = WooPosCornerRadius.Large.value,
                    topEnd = WooPosCornerRadius.Large.value,
                ),
                color = MaterialTheme.colorScheme.surfaceBright,
                shadowElevation = WooPosElevation.Medium.value,
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    content()
                }
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosBottomSheetPreview() {
    WooPosTheme {
        WooPosBottomSheet(
            isVisible = true,
            scrimContentDescription = "",
            onDismissRequest = {},
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.XLarge.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WooPosText(
                    text = "Bottom sheet title",
                    style = WooPosTypography.Heading,
                    fontWeight = FontWeight.Bold,
                )
                WooPosText(
                    text = "As tall as its content.",
                    style = WooPosTypography.BodyLarge,
                )
            }
        }
    }
}
