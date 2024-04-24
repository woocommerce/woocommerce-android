package com.woocommerce.android.ui.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.presentation.theme.WooTheme

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    LoginScreen(
        isLoading = viewState?.isLoading ?: false,
        onLoginButtonClicked = viewModel::onLoginButtonClicked
    )
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    onLoginButtonClicked: () -> Unit
) {
    WooTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            if (isLoading) {
                Text("Loading...")
            } else {
                SyncScreen(onLoginButtonClicked)
            }
        }
    }
}

@Composable
fun SyncScreen(
    onLoginButtonClicked: () -> Unit
) {
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

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun Preview() {
    LoginScreen(
        isLoading = false,
        onLoginButtonClicked = {}
    )
}
