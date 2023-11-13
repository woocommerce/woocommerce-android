@file:OptIn(ExperimentalAnimationApi::class)

package com.woocommerce.android.ui.shipping

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState.PreEnter
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState.InstallationOngoing
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState.PreInstallation

@Composable
fun AnimatedVisibilityScope.InstallWCShippingFlow(viewState: InstallationState) {
    when (viewState) {
        is PreInstallation -> PreInstallationContent(viewState)
        is InstallationOngoing -> InstallationContent(viewState)
    }
}

@Composable
private fun AnimatedVisibilityScope.PreInstallationContent(viewState: InstallationState.PreInstallation) {
    val initialOffset = with(LocalDensity.current) { 120.dp.roundToPx() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.major_150)
            )
    ) {
        IconButton(onClick = viewState.onCancelClick) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(id = R.string.cancel)
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .animateEnterExit(
                    enter = slideInVertically(
                        animationSpec =
                        tween(
                            durationMillis = 500,
                            delayMillis = 500,
                            // Ensure a bit of elasticity at the end of the animation
                            easing = CubicBezierEasing(0.7f, 0.6f, 0.74f, 1.3f)
                        ),
                        initialOffsetY = { -initialOffset }
                    ),
                    exit = ExitTransition.None
                )
        ) {
            SpacerWithMinHeight(1f, dimensionResource(id = R.dimen.major_100))
            Box {
                InstallationLoadingIndicator(
                    showLoadingIndicator = false,
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.image_major_120))
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward_rounded),
                    contentDescription = null,
                    tint = colorResource(id = R.color.woo_purple_50),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(dimensionResource(id = R.dimen.image_major_64))
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
            MainContent(viewState)
            SpacerWithMinHeight(0.75f, dimensionResource(id = R.dimen.major_100))

            InstallationInfoLink(
                onClick = viewState.onInfoClick,
                modifier = Modifier
                    .animateEnterExit(
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(500))
                    )
            )

            SpacerWithMinHeight(0.75f, dimensionResource(id = R.dimen.major_100))
        }
        (viewState as? PreInstallation)?.let {
            WCColoredButton(
                onClick = viewState.onProceedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateEnterExit(
                        enter = slideInVertically(
                            animationSpec =
                            tween(
                                durationMillis = 500,
                                delayMillis = 500,
                                // Ensure a bit of elasticity at the end of the animation
                                easing = CubicBezierEasing(0.7f, 0.6f, 0.74f, 1.3f)
                            ),
                            initialOffsetY = { initialOffset }
                        ),
                        exit = ExitTransition.None
                    )
            ) {
                Text(text = stringResource(id = R.string.install_wc_shipping_proceed_button))
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.InstallationContent(viewState: InstallationOngoing) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.major_150)
            )
    ) {
        // fill equivalent space as the cross-icon
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_300)))
        SpacerWithMinHeight(1f, dimensionResource(id = R.dimen.major_100))

        Box(modifier = Modifier.size(dimensionResource(id = R.dimen.image_major_120))) {
            val alpha by transition.animateFloat(
                transitionSpec = { tween(1000, delayMillis = 1000) },
                label = "arrowAlpha"
            ) {
                when (it) {
                    PreEnter -> 1f
                    else -> 0f
                }
            }

            val rotation by transition.animateFloat(
                transitionSpec = {
                    keyframes {
                        durationMillis = 2000
                        0f at 1000
                        -20f at 1100
                        180f at 2000
                    }
                },
                label = "arrowAlpha"
            ) {
                when (it) {
                    PreEnter -> 0f
                    else -> 180f
                }
            }
            val isShowingLoadingIndicator by remember { derivedStateOf { alpha == 0f } }

            Box {
                InstallationLoadingIndicator(
                    showLoadingIndicator = isShowingLoadingIndicator,
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.image_major_120))
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward_rounded),
                    contentDescription = null,
                    tint = colorResource(id = R.color.woo_purple_50),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .alpha(alpha)
                        .rotate(rotation)
                        .size(dimensionResource(id = R.dimen.image_major_64))
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
        MainContent(viewState)
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_125)))
        Text(
            text = viewState.siteUrl,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .animateEnterExit(enter = fadeIn(tween(400, delayMillis = 600), initialAlpha = 0.5f))
        )
        SpacerWithMinHeight(1.5f, dimensionResource(id = R.dimen.major_100))
        // fill equivalent space as the proceed-button
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_300)))
    }
}

@Composable
private fun AnimatedVisibilityScope.MainContent(viewState: InstallationState) {
    Column {
        val text = when (viewState) {
            is PreInstallation -> stringResource(id = R.string.install_wc_shipping_preinstall_title)
            is InstallationOngoing -> "Installing"
        }
        Text(
            text = text,
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            // Animate the step title when starting the installation
            modifier = Modifier.animateEnterExit(
                enter = if (viewState is InstallationOngoing) {
                    fadeIn(tween(400, delayMillis = 600, easing = LinearEasing))
                } else EnterTransition.None,
                exit = ExitTransition.None
            )
        )

        // Animate the extension and site names when starting the installation
        val extensionAndNameModifier = Modifier.animateEnterExit(
            enter = if (viewState is InstallationOngoing) {
                fadeIn(
                    keyframes {
                        durationMillis = 1000
                        1f at 0
                        0.5f at 200
                        0.5f at 800
                        1f at 1000
                    }
                )
            } else EnterTransition.None,
            exit = ExitTransition.None
        )

        Text(
            text = stringResource(id = viewState.extensionsName),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.woo_purple_50),
            modifier = extensionAndNameModifier
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
        Text(
            text = viewState.siteName,
            style = MaterialTheme.typography.h4,
            modifier = extensionAndNameModifier
        )
    }
}

@Composable
private fun InstallationInfoLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = colorResource(id = R.color.link_text)
        )
        Text(
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.link_text),
            text = stringResource(id = R.string.install_wc_shipping_installation_info),
        )
    }
}

@Composable
private fun ColumnScope.SpacerWithMinHeight(weight: Float, minHeight: Dp) {
    Spacer(modifier = Modifier.height(minHeight))
    Spacer(modifier = Modifier.weight(weight))
}

@Composable
private fun InstallationLoadingIndicator(showLoadingIndicator: Boolean, modifier: Modifier = Modifier) {
    val stroke = with(LocalDensity.current) {
        Stroke(width = dimensionResource(id = R.dimen.major_75).toPx(), cap = StrokeCap.Round)
    }

    val circleColor = colorResource(id = R.color.woo_purple_20)
    val progressColor = colorResource(id = R.color.woo_purple_50)

    val startAngle by if (showLoadingIndicator) {
        val transition = rememberInfiniteTransition(label = "")

        transition.animateFloat(
            initialValue = -90f,
            targetValue = 270f,
            animationSpec = infiniteRepeatable(
                animation = tween(1332, easing = LinearEasing)
            ),
            label = ""
        )
    } else {
        remember { mutableStateOf(-90f) }
    }

    Canvas(modifier) {
        val size = size.width - stroke.width

        drawCircle(
            color = circleColor,
            radius = (size) / 2,
            style = stroke
        )
        if (showLoadingIndicator) {
            drawArc(
                color = progressColor,
                startAngle = startAngle,
                sweepAngle = 30f,
                useCenter = false,
                size = Size(size, size),
                topLeft = Offset(stroke.width / 2, stroke.width / 2),
                style = stroke
            )
        }
    }
}

@Preview
@Composable
@Suppress("UnusedContentLambdaTargetStateParameter")
private fun PreInstallationPreview() {
    WooThemeWithBackground {
        AnimatedContent(targetState = Unit, label = "") {
            InstallWCShippingFlow(
                viewState = PreInstallation(
                    extensionsName = R.string.install_wc_shipping_extension_name,
                    siteName = "Site",
                    siteUrl = "URL",
                    onCancelClick = {},
                    onProceedClick = {},
                    onInfoClick = {}
                )
            )
        }
    }
}

@Preview
@Composable
@Suppress("UnusedContentLambdaTargetStateParameter")
private fun InstallationOngoingPreview() {
    WooThemeWithBackground {
        AnimatedContent(targetState = Unit, label = "") {
            InstallationContent(
                viewState = InstallationOngoing(
                    extensionsName = R.string.install_wc_shipping_extension_name,
                    siteName = "Site",
                    siteUrl = "URL"
                )
            )
        }
    }
}
