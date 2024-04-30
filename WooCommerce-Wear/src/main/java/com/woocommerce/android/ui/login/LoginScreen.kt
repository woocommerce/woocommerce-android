package com.woocommerce.android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.presentation.theme.WooTheme
import com.woocommerce.android.presentation.theme.WooTypography

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
            if (isLoading) {
                TimeText()
                CircularProgressIndicator()
                Text("Loading")
            } else {
                SyncScreen(onLoginButtonClicked)
            }
        }
    }
}

@Composable
fun SyncScreen(
    onLoginButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Log in",
            style = WooTypography.body1
        )
        Text(
            text = "Something went wrong",
            style = WooTypography.body2
        )
        Text(
            text = "Make sure your phone is nearby with the Woo app installed and Bluetooth is on.",
            textAlign = TextAlign.Center,
            style = WooTypography.caption2
        )
        Button(
            onClick = onLoginButtonClicked,
            modifier = modifier.fillMaxWidth()
        ) {
            Text("Try Again")
        }
    }
}

@Preview(name = "Error Round", device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(name = "Error Square", device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun PreviewError() {
    LoginScreen(
        isLoading = false,
        onLoginButtonClicked = {}
    )
}

@Preview(name = "Loading Round", device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(name = "Loading Square", device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun PreviewLoading() {
    LoginScreen(
        isLoading = true,
        onLoginButtonClicked = {}
    )
}
