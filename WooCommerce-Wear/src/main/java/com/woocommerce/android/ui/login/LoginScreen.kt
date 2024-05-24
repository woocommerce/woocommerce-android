package com.woocommerce.android.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.R
import com.woocommerce.android.presentation.component.LoadingScreen
import com.woocommerce.android.presentation.theme.WooTheme
import com.woocommerce.android.presentation.theme.WooTypography

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    LoginScreen(
        isLoading = viewState?.isLoading ?: false,
        onTryAgainClicked = viewModel::reloadData
    )
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    onTryAgainClicked: () -> Unit
) {
    WooTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                TimeText()
                LoadingScreen()
            } else {
                LoginErrorScreen(onTryAgainClicked)
            }
        }
    }
}

@Composable
private fun LoginErrorScreen(
    onTryAgainClicked: () -> Unit,
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
            text = stringResource(id = R.string.login_screen_error_title),
            style = WooTypography.body1,
            modifier = modifier.padding(bottom = 16.dp)
        )
        Text(
            text = stringResource(id = R.string.login_screen_error_caption),
            textAlign = TextAlign.Center,
            style = WooTypography.caption1,
            modifier = modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = onTryAgainClicked,
            modifier = modifier.width(150.dp)
        ) {
            Text(stringResource(id = R.string.login_screen_error_action_button))
        }
    }
}

@Preview(name = "Error Round", device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(name = "Error Square", device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun PreviewError() {
    LoginScreen(
        isLoading = false,
        onTryAgainClicked = {}
    )
}

@Preview(name = "Loading Round", device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(name = "Loading Square", device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun PreviewLoading() {
    LoginScreen(
        isLoading = true,
        onTryAgainClicked = {}
    )
}
