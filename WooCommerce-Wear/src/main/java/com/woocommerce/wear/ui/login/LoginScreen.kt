package com.woocommerce.wear.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.wear.presentation.theme.WooTheme

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    LoginScreen(
        onLoginButtonClicked = viewModel::onLoginButtonClicked
    )
}

@Composable
fun LoginScreen(
    onLoginButtonClicked: () -> Unit
) {
    WooTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Button(
                    onClick = onLoginButtonClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue on phone")
                }
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun Preview() {
    LoginScreen(
        onLoginButtonClicked = {}
    )
}
