package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing

private const val DEFAULT_WIDTH_FRACTION = 0.75f
private const val ENTER_EXIT_DURATION_MS = 300

@Composable
fun WooPosDialogWrapper(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    dialogBackgroundContentDescription: String,
    widthFraction: Float = DEFAULT_WIDTH_FRACTION,
    onCloseClick: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val closeContentDescription = stringResource(R.string.close)

    BackHandler(enabled = isVisible) { onDismissRequest() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
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
            enter = fadeIn(animationSpec = tween(ENTER_EXIT_DURATION_MS)) + slideInVertically(
                initialOffsetY = { it / 8 },
                animationSpec = tween(ENTER_EXIT_DURATION_MS)
            ),
            exit = fadeOut(animationSpec = tween(ENTER_EXIT_DURATION_MS)) + slideOutVertically(
                targetOffsetY = { it / 8 },
                animationSpec = tween(ENTER_EXIT_DURATION_MS)
            ),
        ) {
            val isFullyVisible =
                transition.currentState == EnterExitState.Visible &&
                    transition.targetState == EnterExitState.Visible

            WooPosCard(
                shape = RoundedCornerShape(WooPosCornerRadius.Large.value),
                backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                elevation = if (isFullyVisible) WooPosElevation.Medium else WooPosElevation.None,
                modifier = modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .fillMaxWidth(widthFraction),
            ) {
                Column(
                    modifier = Modifier.padding(WooPosSpacing.XLarge.value)
                ) {
                    if (onCloseClick != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = onCloseClick,
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                                    contentDescription = closeContentDescription,
                                    modifier = Modifier.size(WooPosIconSize.Large.value),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.size(WooPosSpacing.XLarge.value))
                    }

                    content()
                }
            }
        }
    }
}
