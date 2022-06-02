package com.woocommerce.android.ui.shipping

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.InstallWcShippingOnboardingBulletUi
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.InstallWcShippingOnboardingUi
import com.woocommerce.android.ui.shipping.InstallWcShippingFlowViewModel.InstallWcShippingState

@Composable
fun InstallWcShippingOnboardingScreen(viewmodel: InstallWcShippingFlowViewModel) {
    val installWcShippingFlowState by viewmodel.installWcShippingFlowState.observeAsState(InstallWcShippingState())
    installWcShippingFlowState.installWcShippingOnboardingUi?.let {
        InstallWcShippingOnboardingScreen(it)
    }
}

@Composable
fun InstallWcShippingOnboardingScreen(onboardingUi: InstallWcShippingOnboardingUi) {
    Column {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                style = MaterialTheme.typography.h5,
                text = stringResource(id = R.string.install_wc_shipping_flow_onboarding_screen_title)
            )
            Text(
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                text = stringResource(id = R.string.install_wc_shipping_flow_onboarding_screen_title)
            )
            InstallWcsOnboardingBullets(onboardingBullets = onboardingUi.bullets)
        }
        Text(
            style = MaterialTheme.typography.h5,
            text = stringResource(id = R.string.install_wc_shipping_flow_onboarding_screen_link)
        )
        Button(onClick = { /*TODO*/ }) {
            Text(
                text = stringResource(id = R.string.install_wc_shipping_flow_onboarding_screen_add_extension_button)
            )
        }
        Button(onClick = { /*TODO*/ }) {
            Text(
                text = stringResource(id = R.string.install_wc_shipping_flow_onboarding_screen_not_now_button)
            )
        }
    }
}

@Composable
fun InstallWcsOnboardingBullets(
    modifier: Modifier = Modifier,
    onboardingBullets: List<InstallWcShippingOnboardingBulletUi>
) {
    Column(modifier) {
        onboardingBullets.forEach {
            InstallWcsOnboardingBullet(bullet = it)
        }
    }
}

@Composable
fun InstallWcsOnboardingBullet(
    modifier: Modifier = Modifier,
    bullet: InstallWcShippingOnboardingBulletUi
) {
    Row(modifier) {
        Image(
            painter = painterResource(id = bullet.icon),
            contentDescription = null,
        )
        Column {
            Text(
                style = MaterialTheme.typography.subtitle1,
                text = stringResource(id = bullet.title)
            )
            Text(
                style = MaterialTheme.typography.body2,
                text = stringResource(id = bullet.description)
            )
        }
    }
}
