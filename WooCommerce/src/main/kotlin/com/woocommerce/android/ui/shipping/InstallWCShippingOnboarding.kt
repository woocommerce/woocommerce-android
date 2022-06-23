@file:OptIn(ExperimentalAnimationApi::class)

package com.woocommerce.android.ui.shipping

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.InstallWCShippingOnboardingBulletUi
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.Onboarding

@Composable
fun AnimatedVisibilityScope.InstallWcShippingOnboarding(viewState: Onboarding) {
    val targetExitOffset = with(LocalDensity.current) { 120.dp.roundToPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = dimensionResource(id = R.dimen.major_200),
                end = dimensionResource(id = R.dimen.major_200)
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .animateEnterExit(
                    enter = EnterTransition.None,
                    exit = slideOutVertically(
                        animationSpec =
                        tween(durationMillis = 500),
                        targetOffsetY = { -targetExitOffset }
                    )
                )
        ) {
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_350)),
                style = MaterialTheme.typography.h5,
                text = stringResource(viewState.title),
                textAlign = TextAlign.Center,
                fontSize = 28.sp,
                lineHeight = 34.sp,
            )
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                text = stringResource(viewState.subtitle),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
            InstallWcsOnboardingBullets(onboardingBullets = viewState.bullets)
            LinkText(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_200)),
                text = stringResource(R.string.install_wc_shipping_flow_onboarding_screen_link),
                onClick = viewState.onInfoLinkClicked
            )
        }
        Column(
            modifier = Modifier
                .padding(
                    top = dimensionResource(id = R.dimen.major_200),
                    bottom = dimensionResource(id = R.dimen.major_200),
                )
                .animateEnterExit(
                    enter = EnterTransition.None,
                    exit = slideOutVertically(
                        animationSpec =
                        tween(durationMillis = 500),
                        targetOffsetY = { targetExitOffset }
                    )
                )
        ) {
            WCColoredButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewState.onInstallClicked() },
            ) {
                Text(
                    text = stringResource(R.string.install_wc_shipping_flow_onboarding_screen_add_extension_button),
                )
            }
            Spacer(Modifier.size(dimensionResource(id = R.dimen.major_100)))
            WCOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewState.onDismissFlowClicked() },
            ) {
                Text(
                    text = stringResource(R.string.install_wc_shipping_flow_onboarding_screen_not_now_button),
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.color_on_surface_high),
                )
            }
        }
    }
}

@Composable
private fun LinkText(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit
) {
    Text(
        modifier = modifier.clickable(onClick = onClick),
        style = MaterialTheme.typography.subtitle1,
        color = colorResource(id = R.color.link_text),
        text = text,
    )
}

@Composable
private fun InstallWcsOnboardingBullets(onboardingBullets: List<InstallWCShippingOnboardingBulletUi>) {
    onboardingBullets.forEach {
        InstallWcsOnboardingBullet(bullet = it, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_100)))
    }
}

@Composable
private fun InstallWcsOnboardingBullet(
    bullet: InstallWCShippingOnboardingBulletUi,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        Image(
            painter = painterResource(id = bullet.icon),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))
        Column {
            Text(
                style = MaterialTheme.typography.subtitle1,
                text = stringResource(bullet.title),
                fontWeight = FontWeight.Bold
            )
            Text(
                style = MaterialTheme.typography.body2,
                text = stringResource(bullet.description)
            )
        }
    }
}
