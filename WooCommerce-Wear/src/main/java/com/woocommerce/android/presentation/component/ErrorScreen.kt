package com.woocommerce.android.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.woocommerce.android.R

@Composable
fun ErrorScreen(
    errorText: String,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = errorText,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(8.dp)
        )
        Button(
            onClick = onRetryClicked,
            modifier = modifier.padding(8.dp)
        ) {
            Text(stringResource(id = R.string.error_screen_retry_button))
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun ErrorScreenPreview() {
    ErrorScreen(
        errorText = "An error occurred",
        onRetryClicked = { }
    )
}
