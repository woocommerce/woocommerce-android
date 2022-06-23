@file:OptIn(ExperimentalAnimationApi::class)

package com.woocommerce.android.ui.shipping

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.EnterTransition.Companion
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState.InstallationOngoing
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState.PreInstallation

@OptIn(ExperimentalTransitionApi::class)
@Composable
fun AnimatedVisibilityScope.InstallWCShippingFlow(viewState: InstallationState) {
    when (viewState) {
        is PreInstallation -> PreInstallationContent(viewState)
        is InstallationOngoing -> TODO()
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
        (viewState as? PreInstallation)?.let {
            IconButton(onClick = viewState.onCancelClick) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(id = R.string.cancel)
                )
            }
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
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(
                        width = dimensionResource(id = R.dimen.major_75),
                        color = colorResource(id = R.color.woo_purple_20),
                        shape = CircleShape
                    )
                    .size(dimensionResource(id = R.dimen.image_major_120))
            ) {
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
            AnimatedVisibility(visible = viewState is PreInstallation) {
                InstallationInfoLink { (viewState as? PreInstallation)?.onInfoClick?.invoke() }
            }
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedVisibilityScope.MainContent(viewState: InstallationState) {
    Column {
        val text = when (viewState) {
            is PreInstallation -> stringResource(id = string.install_wc_shipping_preinstall_title)
            is InstallationOngoing -> "Installing"
        }
        Text(
            text = text,
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.animateEnterExit(
                enter = if (viewState is InstallationOngoing) {
                    fadeIn(tween(500, delayMillis = 100))
                } else EnterTransition.None,
                exit = ExitTransition.None
            )
        )

        Text(
            text = stringResource(id = viewState.extensionsName),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.woo_purple_50)
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
        Text(
            text = viewState.siteName,
            style = MaterialTheme.typography.h4
        )
    }
}

@Composable
private fun InstallationInfoLink(onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        modifier = Modifier
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

@Preview
@Composable
private fun PreInstallationPreview() {
    WooThemeWithBackground {
        AnimatedContent(targetState = Unit) {
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
