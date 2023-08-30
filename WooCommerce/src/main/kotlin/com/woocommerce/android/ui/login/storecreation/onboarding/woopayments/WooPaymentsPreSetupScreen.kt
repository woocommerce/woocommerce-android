package com.woocommerce.android.ui.login.storecreation.onboarding.woopayments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.compose.component.Toolbar

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
    modifier: Modifier = Modifier
) {

}

@Composable
fun WooPaymentsPreSetupFooter() {

}
