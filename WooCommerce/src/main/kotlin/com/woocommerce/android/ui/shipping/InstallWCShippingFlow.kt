package com.woocommerce.android.ui.shipping

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState
import com.woocommerce.android.ui.shipping.InstallWCShippingViewModel.ViewState.InstallationState.PreInstallation

@Composable
fun InstallWCShippingFlow(viewState: InstallationState, transition: Transition<Boolean>? = null) {
    when (viewState) {
        is PreInstallation -> PreInstallationContent(viewState, transition)
    }
}

@Composable
private fun PreInstallationContent(viewState: PreInstallation, transition: Transition<Boolean>? = null) {
    val offset by transition?.animateInt(
        transitionSpec = {
            tween(
                durationMillis = 500,
                delayMillis = 500,
                // Ensure a bit of elasticity at the end of the animation
                easing = CubicBezierEasing(0.7f, 0.6f, 0.74f, 1.3f)
            )
        },
        label = "offset"
    ) {
        if (it) 0 else 120
    } ?: remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.major_150)
            )
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .offset(y = -offset.dp)
        ) {
            WCTextButton(onClick = viewState.onCancelClick) {
                Text(text = stringResource(id = R.string.cancel))
            }
            Spacer(modifier = Modifier.weight(1f))
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
            Text(
                text = stringResource(id = R.string.install_wc_shipping_preinstall_title),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
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
            Spacer(modifier = Modifier.weight(0.75f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
                modifier = Modifier
                    .clickable(onClick = viewState.onInfoClick)
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
            Spacer(modifier = Modifier.weight(0.75f))
        }
        WCColoredButton(
            onClick = viewState.onProceedClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offset.dp)
        ) {
            Text(text = stringResource(id = R.string.install_wc_shipping_proceed_button))
        }
    }
}

@Preview
@Composable
private fun PreInstallationPreview() {
    WooThemeWithBackground {
        PreInstallationContent(
            viewState = PreInstallation(
                extensionsName = R.string.install_wc_shipping_extension_name,
                siteName = "Site",
                onCancelClick = {},
                onProceedClick = {},
                onInfoClick = {}
            )
        )
    }
}
