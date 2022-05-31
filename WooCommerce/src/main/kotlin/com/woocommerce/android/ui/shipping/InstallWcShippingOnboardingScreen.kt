package com.woocommerce.android.ui.shipping

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R

@Composable
fun ShippingPluginInstallOnboardingScreen() {
    Column {
        Column {
            Text(
                style = MaterialTheme.typography.h5,
                text = stringResource(id = R.string.install_wc_shipping_flow_onboarding_screen_title)
            )
        }
    }
}
