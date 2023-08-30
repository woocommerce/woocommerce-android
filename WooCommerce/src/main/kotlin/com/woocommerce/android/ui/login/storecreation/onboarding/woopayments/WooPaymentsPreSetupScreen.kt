package com.woocommerce.android.ui.login.storecreation.onboarding.woopayments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton

@Composable
fun WooPaymentsPreSetupScreen() {
    Scaffold(
        topBar = {
            Toolbar(
                title = { Text("") },
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationButtonClick = { },
            )
        },
        bottomBar = { WooPaymentsPreSetupFooter() }
    ) { paddingValues ->
        WooPaymentsPreSetupContent(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
        )

    }
}

@Composable
fun WooPaymentsPreSetupContent(
    modifier: Modifier
) {
    Column(
        modifier = modifier
    ) {

    }

}

@Composable
fun WooPaymentsPreSetupFooter() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            onClick = { },
        ) {
            Text(text = stringResource(id = R.string.store_onboarding_wcpay_pre_setup_content_button))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp)
            )
            Text(
                text = stringResource(id = R.string.store_onboarding_wcpay_pre_setup_content_learn_more),
                style = MaterialTheme.typography.subtitle1,
            )
        }
    }
}
