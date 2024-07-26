package com.woocommerce.android.ui.woopos.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding

@Composable
fun ProductInfoDialog(
    state: WooPosHomeState.ProductsInfoDialog.Visible,
    onDismissRequest: () -> Unit,
    onCreateOrderClick: () -> Unit
) {
    val animVisibleState = remember { MutableTransitionState(false) }
        .apply { targetState = true }
    LaunchedEffect(animVisibleState) {
        snapshotFlow { animVisibleState.isIdle && !animVisibleState.currentState }
            .collect { isVisible ->
                if (isVisible) {
                    onDismissRequest()
                }
            }
    }
    AnimatedVisibility(
        visibleState = animVisibleState,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { it / 8 }, // Slide in from 1/4 below the center
            animationSpec = tween(300)
        ),
        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
            targetOffsetY = { it / 8 }, // Slide out to 1/4 below the center
            animationSpec = tween(300)
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 100.dp.toAdaptivePadding(),
                    end = 100.dp.toAdaptivePadding(),
                    top = 100.dp.toAdaptivePadding(),
                    bottom = 100.dp.toAdaptivePadding()
                )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = 8.dp,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp.toAdaptivePadding()),
                        contentAlignment = Alignment.Center
                    ) {
                        ConstraintLayout(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val (header, closeIcon, content) = createRefs()

                            Text(
                                text = stringResource(id = state.header),
                                style = MaterialTheme.typography.h4,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onBackground.copy(alpha = 0.87f),
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .constrainAs(header) {
                                        top.linkTo(closeIcon.bottom)
                                        start.linkTo(parent.start)
                                        end.linkTo(parent.end)
                                        width = Dimension.preferredWrapContent
                                    }
                            )

                            IconButton(
                                onClick = {
                                    animVisibleState.targetState = false
                                },
                                modifier = Modifier.constrainAs(closeIcon) {
                                    top.linkTo(parent.top)
                                    end.linkTo(parent.end)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    tint = MaterialTheme.colors.onSurface,
                                    contentDescription = stringResource(
                                        id = R.string.woopos_banner_simple_products_close_content_description
                                    ),
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.constrainAs(content) {
                                    top.linkTo(header.bottom)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                }
                            ) {
                                Text(
                                    text = stringResource(id = state.primaryMessage),
                                    style = TextStyle(
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 24.sp,
                                        lineHeight = 32.sp
                                    ),
                                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.87f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Box(
                                    Modifier
                                        .background(color = Color(0xF6F7F7).copy(alpha = 0.8f))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = stringResource(id = state.secondaryMessage),
                                            style = MaterialTheme.typography.subtitle1,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.87f),
                                        )
                                        TextButton(
                                            onClick = onCreateOrderClick,
                                        ) {
                                            Text(
                                                text = stringResource(id = state.secondaryMessageActionLabel),
                                                style = MaterialTheme.typography.subtitle1,
                                                color = MaterialTheme.colors.primary,
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        animVisibleState.targetState = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(2.dp, MaterialTheme.colors.primary),
                                    shape = RectangleShape,
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .padding(
                                                top = 8.dp.toAdaptivePadding(),
                                                bottom = 8.dp.toAdaptivePadding(),
                                            ),
                                        text = stringResource(id = state.primaryButton.label)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@WooPosPreview
@Composable
fun ProductInfoDialogPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ProductInfoDialog(
                state = WooPosHomeState.ProductsInfoDialog.Visible(
                    header = R.string.woopos_dialog_products_info_heading,
                    primaryMessage = R.string.woopos_dialog_products_info_primary_message,
                    secondaryMessage = R.string.woopos_dialog_products_info_secondary_message,
                    secondaryMessageActionLabel = R.string.woopos_dialog_products_info_secondary_message_action_label,
                    primaryButton = WooPosHomeState.ProductsInfoDialog.Visible.PrimaryButton(
                        label = R.string.woopos_dialog_products_info_button_label
                    )
                ),
                onDismissRequest = {},
                onCreateOrderClick = {}
            )
        }
    }
}

